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

package org.apache.inlong.sort.iceberg.sink.multiple;

import org.apache.inlong.sort.base.dirty.DirtyData;
import org.apache.inlong.sort.base.dirty.DirtyOptions;
import org.apache.inlong.sort.base.dirty.sink.DirtySink;
import org.apache.inlong.sort.base.metric.MetricOption;
import org.apache.inlong.sort.base.metric.MetricOption.RegisteredMetric;
import org.apache.inlong.sort.base.metric.MetricState;
import org.apache.inlong.sort.base.metric.SinkMetricData;
import org.apache.inlong.sort.base.util.MetricStateUtils;
import org.apache.inlong.sort.iceberg.schema.IcebergModeSwitchHelper;
import org.apache.inlong.sort.iceberg.sink.RowDataTaskWriterFactory;

import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.logical.RowType;
import org.apache.iceberg.flink.sink.TaskWriterFactory;
import org.apache.iceberg.io.TaskWriter;
import org.apache.iceberg.io.WriteResult;
import org.apache.iceberg.relocated.com.google.common.base.MoreObjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.apache.inlong.sort.base.Constants.DIRTY_BYTES_OUT;
import static org.apache.inlong.sort.base.Constants.DIRTY_RECORDS_OUT;
import static org.apache.inlong.sort.base.Constants.INLONG_METRIC_STATE_NAME;
import static org.apache.inlong.sort.base.Constants.NUM_BYTES_OUT;
import static org.apache.inlong.sort.base.Constants.NUM_RECORDS_OUT;
import static org.apache.inlong.sort.iceberg.schema.IcebergModeSwitchHelper.DEFAULT_META_INDEX;

public class IcebergSingleStreamWriter<T> extends IcebergProcessFunction<T, WriteResult>
        implements
            CheckpointedFunction,
            SchemaEvolutionFunction<TaskWriterFactory<T>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(IcebergSingleStreamWriter.class);

    private static final long serialVersionUID = 1L;

    private final String fullTableName;
    private final String inlongMetric;
    private final String auditHostAndPorts;
    private final String auditKeys;
    private RowDataTaskWriterFactory taskWriterFactory;

    private transient TaskWriter<RowData> writer;

    private transient int subTaskId;
    private transient int attemptId;
    private @Nullable transient SinkMetricData metricData;
    private transient ListState<MetricState> metricStateListState;
    private transient MetricState metricState;
    private @Nullable RowType flinkRowType;
    private final DirtyOptions dirtyOptions;
    private @Nullable final DirtySink<Object> dirtySink;
    private boolean multipleSink;
    private final RowType tableSchemaRowType;
    private final int incrementalFieldIndex;
    private final List<WriteResult> cachedWriteResults;
    private final boolean switchAppendUpsertEnable;
    private IcebergModeSwitchHelper switchHelper;

    public IcebergSingleStreamWriter(
            String fullTableName,
            RowDataTaskWriterFactory taskWriterFactory,
            String inlongMetric,
            String auditHostAndPorts,
            @Nullable RowType flinkRowType,
            DirtyOptions dirtyOptions,
            @Nullable DirtySink<Object> dirtySink,
            boolean multipleSink,
            RowType tableSchemaRowType,
            int incrementalFieldIndex,
            boolean switchAppendUpsertEnable,
            String auditKeys) {
        this.fullTableName = fullTableName;
        this.taskWriterFactory = taskWriterFactory;
        this.inlongMetric = inlongMetric;
        this.auditHostAndPorts = auditHostAndPorts;
        this.flinkRowType = flinkRowType;
        this.dirtyOptions = dirtyOptions;
        this.dirtySink = dirtySink;
        this.multipleSink = multipleSink;
        this.tableSchemaRowType = tableSchemaRowType;
        this.incrementalFieldIndex = incrementalFieldIndex;
        this.cachedWriteResults = new ArrayList<>();
        this.switchAppendUpsertEnable = switchAppendUpsertEnable;
        this.auditKeys = auditKeys;
    }

    public RowType getFlinkRowType() {
        return flinkRowType;
    }

    @Override
    public void open(Configuration parameters) {
        this.subTaskId = getRuntimeContext().getIndexOfThisSubtask();
        this.attemptId = getRuntimeContext().getAttemptNumber();

        // Initialize the task writer factory.
        this.taskWriterFactory.initialize(subTaskId, attemptId);
        // Initialize the task writer.
        createTaskWriter();

        switchHelper = new IcebergModeSwitchHelper(tableSchemaRowType, incrementalFieldIndex);

        // Initialize metric
        if (!multipleSink) {
            MetricOption metricOption = MetricOption.builder()
                    .withInlongLabels(inlongMetric)
                    .withAuditAddress(auditHostAndPorts)
                    .withInitRecords(metricState != null ? metricState.getMetricValue(NUM_RECORDS_OUT) : 0L)
                    .withInitBytes(metricState != null ? metricState.getMetricValue(NUM_BYTES_OUT) : 0L)
                    .withInitDirtyRecords(metricState != null ? metricState.getMetricValue(DIRTY_RECORDS_OUT) : 0L)
                    .withInitDirtyBytes(metricState != null ? metricState.getMetricValue(DIRTY_BYTES_OUT) : 0L)
                    .withRegisterMetric(RegisteredMetric.ALL)
                    .withAuditKeys(auditKeys)
                    .build();
            if (metricOption != null) {
                metricData = new SinkMetricData(metricOption, getRuntimeContext().getMetricGroup());
            }

            if (dirtySink != null) {
                try {
                    dirtySink.open(new Configuration());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * this method should only be called in open() method
     */
    private void createTaskWriter() {
        if (switchAppendUpsertEnable) {
            // when the job starts and the switch is enabled, the writer
            // should be in append mode by default
            taskWriterFactory.switchToAppend();
        }
        this.writer = taskWriterFactory.create();
    }

    @Override
    public void prepareSnapshotPreBarrier(long checkpointId) throws Exception {
        // submit the cached write results
        LOGGER.info("Submit {} cached write results before checkpoint {}.",
                cachedWriteResults.size(), checkpointId);
        cachedWriteResults.forEach(this::emit);
        cachedWriteResults.clear();
        // close all open files and emit files to downstream committer operator
        emit(writer.complete());
        this.writer = taskWriterFactory.create();
    }

    private void cacheWriteResultAndRecreateWriter() throws IOException {
        LOGGER.info("close all open file and cache writeResult");
        cachedWriteResults.add(writer.complete());
        this.writer = taskWriterFactory.create();
    }

    public void switchToUpsert() throws Exception {
        if (!taskWriterFactory.isUpsert()) {
            LOGGER.info("iceberg writer switch to upsert write mode");
            taskWriterFactory.switchToUpsert();
            cacheWriteResultAndRecreateWriter();
        }
    }

    @Override
    public void processElement(T value) throws Exception {

        try {
            if (disableSwitch()) {
                writer.write((RowData) value);
            } else {
                if (isIncrementalPhase((RowData) value)) {
                    switchToUpsert();
                }
                writer.write(switchHelper.removeIncrementalField((RowData) value));
            }
        } catch (Exception e) {
            if (multipleSink) {
                throw e;
            }

            LOGGER.error(String.format("write error, raw data: %s", value), e);
            if (!dirtyOptions.ignoreDirty()) {
                throw e;
            }
            if (dirtySink != null) {
                DirtyData.Builder<Object> builder = DirtyData.builder();
                if (!disableSwitch()) {
                    value = (T) switchHelper.removeIncrementalField((RowData) value);
                }
                try {
                    builder.setData(value)
                            .setLabels(dirtyOptions.getLabels())
                            .setLogTag(dirtyOptions.getLogTag())
                            .setIdentifier(dirtyOptions.getIdentifier())
                            .setRowType(flinkRowType)
                            .setDirtyMessage(e.getMessage());
                    dirtySink.invoke(builder.build());
                    if (metricData != null) {
                        metricData.invokeDirtyWithEstimate(value);
                    }
                } catch (Exception ex) {
                    if (!dirtyOptions.ignoreSideOutputErrors()) {
                        throw new RuntimeException(ex);
                    }
                    LOGGER.warn("Dirty sink failed", ex);
                }
            }
            return;
        }
        if (metricData != null) {
            metricData.invokeWithEstimate(value);
        }
    }

    /**
     * disable switch when the switch property is disabled
     * or the sink is multiple sink (the data is in json format)
     * or the incremental field is not set
     */
    private boolean disableSwitch() {
        return !switchAppendUpsertEnable || multipleSink || incrementalFieldIndex == DEFAULT_META_INDEX;
    }

    /**
     * check if the data is incremental phase
     * by checking the incremental field
     */
    private boolean isIncrementalPhase(RowData rowData) {
        return rowData.getBoolean(incrementalFieldIndex);
    }

    @Override
    public void initializeState(FunctionInitializationContext context) throws Exception {
        // init metric state
        if (multipleSink) {
            return;
        }
        if (this.inlongMetric != null) {
            this.metricStateListState = context.getOperatorStateStore().getUnionListState(
                    new ListStateDescriptor<>(
                            String.format(INLONG_METRIC_STATE_NAME, fullTableName),
                            TypeInformation.of(new TypeHint<MetricState>() {
                            })));
        }
        if (context.isRestored()) {
            metricState = MetricStateUtils.restoreMetricState(metricStateListState,
                    getRuntimeContext().getIndexOfThisSubtask(), getRuntimeContext().getNumberOfParallelSubtasks());
        }
    }

    public void setFlinkRowType(@Nullable RowType flinkRowType) {
        this.flinkRowType = flinkRowType;
    }

    @Override
    public void snapshotState(FunctionSnapshotContext context) throws Exception {
        if (metricData != null && metricStateListState != null) {
            MetricStateUtils.snapshotMetricStateForSinkMetricData(metricStateListState, metricData,
                    getRuntimeContext().getIndexOfThisSubtask());
        }
    }

    @Override
    public void dispose() throws Exception {
        if (writer != null) {
            writer.close();
            writer = null;
        }
    }

    @Override
    public void endInput() throws IOException {
        // For bounded stream, it may don't enable the checkpoint mechanism so we'd better to emit the remaining
        // completed files to downstream before closing the writer so that we won't miss any of them.
        emit(writer.complete());
    }

    @Override
    public void schemaEvolution(TaskWriterFactory<T> schema) throws IOException {
        emit(writer.complete());

        taskWriterFactory = (RowDataTaskWriterFactory) schema;
        taskWriterFactory.initialize(subTaskId, attemptId);
        writer = taskWriterFactory.create();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("table_name", fullTableName)
                .add("subtask_id", subTaskId)
                .add("attempt_id", attemptId)
                .toString();
    }

    private void emit(WriteResult result) {
        LOGGER.debug("Emit iceberg write result dataFiles: {}, result.deleteFiles {}",
                result.dataFiles(), result.deleteFiles());
        collector.collect(result);
    }

}
