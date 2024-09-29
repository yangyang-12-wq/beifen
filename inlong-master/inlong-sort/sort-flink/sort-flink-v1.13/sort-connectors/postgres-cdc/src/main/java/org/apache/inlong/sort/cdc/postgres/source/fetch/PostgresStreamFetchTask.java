/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.cdc.postgres.source.fetch;

import org.apache.inlong.sort.cdc.base.relational.JdbcSourceEventDispatcher;
import org.apache.inlong.sort.cdc.base.source.meta.split.SourceSplitBase;
import org.apache.inlong.sort.cdc.base.source.meta.split.StreamSplit;
import org.apache.inlong.sort.cdc.base.source.meta.wartermark.WatermarkKind;
import org.apache.inlong.sort.cdc.base.source.reader.external.FetchTask;
import org.apache.inlong.sort.cdc.postgres.source.offset.PostgresOffset;

import io.debezium.connector.postgresql.PostgresConnectorConfig;
import io.debezium.connector.postgresql.PostgresOffsetContext;
import io.debezium.connector.postgresql.PostgresSchema;
import io.debezium.connector.postgresql.PostgresStreamingChangeEventSource;
import io.debezium.connector.postgresql.PostgresTaskContext;
import io.debezium.connector.postgresql.connection.Lsn;
import io.debezium.connector.postgresql.connection.PostgresConnection;
import io.debezium.connector.postgresql.connection.ReplicationConnection;
import io.debezium.connector.postgresql.spi.Snapshotter;
import io.debezium.pipeline.ErrorHandler;
import io.debezium.pipeline.source.spi.ChangeEventSource;
import io.debezium.util.Clock;
import org.apache.flink.util.FlinkRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.apache.inlong.sort.cdc.postgres.source.offset.PostgresOffset.NO_STOPPING_OFFSET;

/** A {@link FetchTask} implementation for Postgres to read streaming changes. */
public class PostgresStreamFetchTask implements FetchTask<SourceSplitBase> {

    private static final Logger LOG = LoggerFactory.getLogger(PostgresStreamFetchTask.class);

    private final StreamSplit split;
    private volatile boolean taskRunning = false;
    private volatile boolean stopped = false;

    private StreamSplitReadTask streamSplitReadTask;

    private Long lastCommitLsn;

    public PostgresStreamFetchTask(StreamSplit streamSplit) {
        this.split = streamSplit;
    }

    @Override
    public void execute(Context context) throws Exception {
        if (stopped) {
            LOG.debug(
                    "StreamFetchTask for split: {} is already stopped and can not be executed",
                    split);
            return;
        } else {
            LOG.debug("execute StreamFetchTask for split: {}", split);
        }

        PostgresSourceFetchTaskContext sourceFetchContext =
                (PostgresSourceFetchTaskContext) context;
        taskRunning = true;
        streamSplitReadTask =
                new StreamSplitReadTask(
                        sourceFetchContext.getDbzConnectorConfig(),
                        sourceFetchContext.getSnapShotter(),
                        sourceFetchContext.getConnection(),
                        sourceFetchContext.getDispatcher(),
                        sourceFetchContext.getErrorHandler(),
                        sourceFetchContext.getTaskContext().getClock(),
                        sourceFetchContext.getDatabaseSchema(),
                        sourceFetchContext.getTaskContext(),
                        sourceFetchContext.getReplicationConnection(),
                        split);
        StreamSplitChangeEventSourceContext changeEventSourceContext =
                new StreamSplitChangeEventSourceContext();
        streamSplitReadTask.execute(
                changeEventSourceContext, sourceFetchContext.getOffsetContext());
    }

    @Override
    public void stop() {
        LOG.debug("stopping StreamFetchTask for split: {}", split);
        if (streamSplitReadTask != null) {
            ((StreamSplitChangeEventSourceContext) streamSplitReadTask.context).finished();
        }
        stopped = true;
    }

    @Override
    public boolean isRunning() {
        return taskRunning;
    }

    @Override
    public SourceSplitBase getSplit() {
        return split;
    }

    public void commitCurrentOffset() {
        if (streamSplitReadTask != null && streamSplitReadTask.offsetContext != null) {
            PostgresOffsetContext postgresOffsetContext = streamSplitReadTask.offsetContext;

            // only extracting and storing the lsn of the last commit
            Long commitLsn =
                    (Long) postgresOffsetContext
                            .getOffset()
                            .get(PostgresOffsetContext.LAST_COMMIT_LSN_KEY);
            if (commitLsn != null
                    && (lastCommitLsn == null
                            || Lsn.valueOf(commitLsn).compareTo(Lsn.valueOf(lastCommitLsn)) > 0)) {
                lastCommitLsn = commitLsn;

                Map<String, Object> offsets = new HashMap<>();
                offsets.put(PostgresOffsetContext.LAST_COMMIT_LSN_KEY, lastCommitLsn);
                LOG.debug(
                        "Committing offset {} for {}",
                        Lsn.valueOf(lastCommitLsn),
                        streamSplitReadTask.streamSplit);
                streamSplitReadTask.commitOffset(offsets);
            }
        }
    }

    private class StreamSplitChangeEventSourceContext
            implements
                ChangeEventSource.ChangeEventSourceContext {

        public void finished() {
            taskRunning = false;
        }

        @Override
        public boolean isRunning() {
            return taskRunning;
        }
    }

    /** A {@link ChangeEventSource} implementation for Postgres to read streaming changes. */
    public static class StreamSplitReadTask extends PostgresStreamingChangeEventSource {

        private static final Logger LOG = LoggerFactory.getLogger(StreamSplitReadTask.class);
        private final StreamSplit streamSplit;
        private final JdbcSourceEventDispatcher dispatcher;
        private final ErrorHandler errorHandler;

        public ChangeEventSourceContext context;
        public PostgresOffsetContext offsetContext;

        public StreamSplitReadTask(
                PostgresConnectorConfig connectorConfig,
                Snapshotter snapshotter,
                PostgresConnection connection,
                JdbcSourceEventDispatcher dispatcher,
                ErrorHandler errorHandler,
                Clock clock,
                PostgresSchema schema,
                PostgresTaskContext taskContext,
                ReplicationConnection replicationConnection,
                StreamSplit streamSplit) {

            super(
                    connectorConfig,
                    snapshotter,
                    connection,
                    dispatcher,
                    errorHandler,
                    clock,
                    schema,
                    taskContext,
                    replicationConnection);
            this.streamSplit = streamSplit;
            this.dispatcher = dispatcher;
            this.errorHandler = errorHandler;
        }

        @Override
        public void execute(ChangeEventSourceContext context, PostgresOffsetContext offsetContext)
                throws InterruptedException {
            this.context = context;
            this.offsetContext = offsetContext;

            LOG.info("Execute StreamSplitReadTask for split: {}", streamSplit);

            offsetContext.setStreamingStoppingLsn(
                    ((PostgresOffset) streamSplit.getEndingOffset()).getLsn());
            super.execute(context, offsetContext);
            if (isBoundedRead()) {

                LOG.info("StreamSplit is bounded read: {}", streamSplit);
                final PostgresOffset currentOffset = PostgresOffset.of(offsetContext.getOffset());

                if (currentOffset.isAtOrAfter(streamSplit.getEndingOffset())) {
                    LOG.info("StreamSplitReadTask finished for {}", streamSplit);

                    try {
                        dispatcher.dispatchWatermarkEvent(
                                offsetContext.getPartition(),
                                streamSplit,
                                currentOffset,
                                WatermarkKind.END);
                    } catch (InterruptedException e) {
                        LOG.error("Send signal event error.", e);
                        errorHandler.setProducerThrowable(
                                new FlinkRuntimeException("Error processing WAL signal event", e));
                    }

                    ((PostgresScanFetchTask.PostgresChangeEventSourceContext) context).finished();
                }
            }
        }

        private boolean isBoundedRead() {
            return !NO_STOPPING_OFFSET
                    .getLsn()
                    .equals(((PostgresOffset) streamSplit.getEndingOffset()).getLsn());
        }
    }
}
