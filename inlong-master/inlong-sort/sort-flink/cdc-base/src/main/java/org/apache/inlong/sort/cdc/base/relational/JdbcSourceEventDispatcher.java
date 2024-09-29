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

package org.apache.inlong.sort.cdc.base.relational;

import org.apache.inlong.sort.cdc.base.relational.handler.SchemaChangeEventHandler;
import org.apache.inlong.sort.cdc.base.source.meta.offset.Offset;
import org.apache.inlong.sort.cdc.base.source.meta.split.SourceSplitBase;
import org.apache.inlong.sort.cdc.base.source.meta.wartermark.WatermarkEvent;
import org.apache.inlong.sort.cdc.base.source.meta.wartermark.WatermarkKind;

import io.debezium.config.CommonConnectorConfig;
import io.debezium.connector.base.ChangeEventQueue;
import io.debezium.document.DocumentWriter;
import io.debezium.pipeline.DataChangeEvent;
import io.debezium.pipeline.EventDispatcher;
import io.debezium.pipeline.source.spi.EventMetadataProvider;
import io.debezium.pipeline.spi.ChangeEventCreator;
import io.debezium.pipeline.spi.SchemaChangeEventEmitter;
import io.debezium.relational.TableId;
import io.debezium.relational.history.HistoryRecord;
import io.debezium.schema.DataCollectionFilters;
import io.debezium.schema.DatabaseSchema;
import io.debezium.schema.HistorizedDatabaseSchema;
import io.debezium.schema.SchemaChangeEvent;
import io.debezium.schema.TopicSelector;
import io.debezium.util.SchemaNameAdjuster;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * A subclass implementation of {@link EventDispatcher}.
 *
 * <pre>
 *  1. This class shares one {@link ChangeEventQueue} between multiple readers.
 *  2. This class override some methods for dispatching {@link HistoryRecord} directly,
 *     this is useful for downstream to deserialize the {@link HistoryRecord} back.
 * </pre>
 * Copy from com.ververica:flink-cdc-base:2.3.0.
 */
public class JdbcSourceEventDispatcher extends EventDispatcher<TableId> {

    private static final Logger LOG = LoggerFactory.getLogger(JdbcSourceEventDispatcher.class);

    public static final String HISTORY_RECORD_FIELD = "historyRecord";
    public static final String SERVER_ID_KEY = "server_id";
    public static final String BINLOG_FILENAME_OFFSET_KEY = "file";
    public static final String BINLOG_POSITION_OFFSET_KEY = "pos";

    private static final DocumentWriter DOCUMENT_WRITER = DocumentWriter.defaultWriter();

    public final ChangeEventQueue<DataChangeEvent> queue;
    public final HistorizedDatabaseSchema historizedSchema;
    public final DataCollectionFilters.DataCollectionFilter<TableId> filter;
    public final CommonConnectorConfig connectorConfig;
    public final TopicSelector<TableId> topicSelector;
    public final Schema schemaChangeKeySchema;
    public final Schema schemaChangeValueSchema;
    public final String topic;
    private final SchemaChangeEventHandler schemaChangeEventHandler;

    public JdbcSourceEventDispatcher(
            CommonConnectorConfig connectorConfig,
            TopicSelector<TableId> topicSelector,
            DatabaseSchema<TableId> schema,
            ChangeEventQueue<DataChangeEvent> queue,
            DataCollectionFilters.DataCollectionFilter<TableId> filter,
            ChangeEventCreator changeEventCreator,
            EventMetadataProvider metadataProvider,
            SchemaNameAdjuster schemaNameAdjuster,
            SchemaChangeEventHandler schemaChangeEventHandler) {
        super(
                connectorConfig,
                topicSelector,
                schema,
                queue,
                filter,
                changeEventCreator,
                metadataProvider,
                schemaNameAdjuster);
        this.historizedSchema =
                schema instanceof HistorizedDatabaseSchema
                        ? (HistorizedDatabaseSchema<TableId>) schema
                        : null;
        this.filter = filter;
        this.queue = queue;
        this.connectorConfig = connectorConfig;
        this.topicSelector = topicSelector;
        this.topic = topicSelector.getPrimaryTopic();
        this.schemaChangeKeySchema =
                SchemaBuilder.struct()
                        .name(
                                schemaNameAdjuster.adjust(
                                        "io.debezium.connector."
                                                + connectorConfig.getConnectorName()
                                                + ".SchemaChangeKey"))
                        .field(HistoryRecord.Fields.DATABASE_NAME, Schema.STRING_SCHEMA)
                        .build();
        this.schemaChangeValueSchema =
                SchemaBuilder.struct()
                        .name(
                                schemaNameAdjuster.adjust(
                                        "io.debezium.connector."
                                                + connectorConfig.getConnectorName()
                                                + ".SchemaChangeValue"))
                        .field(
                                HistoryRecord.Fields.SOURCE,
                                connectorConfig.getSourceInfoStructMaker().schema())
                        .field(HISTORY_RECORD_FIELD, Schema.OPTIONAL_STRING_SCHEMA)
                        .build();
        this.schemaChangeEventHandler = schemaChangeEventHandler;
    }

    public ChangeEventQueue<DataChangeEvent> getQueue() {
        return queue;
    }

    @Override
    public void dispatchSchemaChangeEvent(
            TableId dataCollectionId, SchemaChangeEventEmitter schemaChangeEventEmitter)
            throws InterruptedException {
        if (dataCollectionId != null && !filter.isIncluded(dataCollectionId)) {
            // TODO `HistorizedSchema.storeOnlyCapturedTables` first appeared in io.debezium.core: 1.6.4-final,
            // which conflicts with io.debezium.core: 15.4-final of cdc-base,
            // so we implement this method separately in the sub-module.
            // if (historizedSchema == null || historizedSchema.storeOnlyCapturedTables()) {
            if (historizedSchema == null || historizedSchema.storeOnlyMonitoredTables()) {
                LOG.trace("Filtering schema change event for {}", dataCollectionId);
                return;
            }
        }
        schemaChangeEventEmitter.emitSchemaChangeEvent(new SchemaChangeEventReceiver());
    }

    @Override
    public void dispatchSchemaChangeEvent(
            Collection<TableId> dataCollectionIds,
            SchemaChangeEventEmitter schemaChangeEventEmitter)
            throws InterruptedException {
        boolean anyNonfilteredEvent = false;
        if (dataCollectionIds == null || dataCollectionIds.isEmpty()) {
            anyNonfilteredEvent = true;
        } else {
            for (TableId dataCollectionId : dataCollectionIds) {
                if (filter.isIncluded(dataCollectionId)) {
                    anyNonfilteredEvent = true;
                    break;
                }
            }
        }
        if (!anyNonfilteredEvent) {
            // TODO `HistorizedSchema.storeOnlyCapturedTables` first appeared in io.debezium.core: 1.6.4-final,
            // which conflicts with io.debezium.core: 15.4-final of cdc-base,
            // so we implement this method separately in the sub-module.
            // if (historizedSchema == null || historizedSchema.storeOnlyCapturedTables()) {
            if (historizedSchema == null || historizedSchema.storeOnlyMonitoredTables()) {
                LOG.trace("Filtering schema change event for {}", dataCollectionIds);
                return;
            }
        }

        schemaChangeEventEmitter.emitSchemaChangeEvent(new SchemaChangeEventReceiver());
    }

    /** A {@link SchemaChangeEventEmitter.Receiver} implementation for {@link SchemaChangeEvent}. */
    public final class SchemaChangeEventReceiver implements SchemaChangeEventEmitter.Receiver {

        private Struct schemaChangeRecordKey(SchemaChangeEvent event) {
            Struct result = new Struct(schemaChangeKeySchema);
            result.put(HistoryRecord.Fields.DATABASE_NAME, event.getDatabase());
            return result;
        }

        private Struct schemaChangeRecordValue(SchemaChangeEvent event) throws IOException {
            Map<String, Object> source = schemaChangeEventHandler.parseSource(event);
            HistoryRecord historyRecord =
                    new HistoryRecord(
                            source,
                            event.getOffset(),
                            event.getDatabase(),
                            event.getSchema(),
                            event.getDdl(),
                            event.getTableChanges());
            String historyStr = DOCUMENT_WRITER.write(historyRecord.document());

            Struct value = new Struct(schemaChangeValueSchema);
            value.put(HistoryRecord.Fields.SOURCE, event.getSource());
            value.put(HISTORY_RECORD_FIELD, historyStr);
            return value;
        }

        @Override
        public void schemaChangeEvent(SchemaChangeEvent event) throws InterruptedException {
            historizedSchema.applySchemaChange(event);
            if (connectorConfig.isSchemaChangesHistoryEnabled()) {
                try {
                    final String topicName = topicSelector.getPrimaryTopic();
                    final Integer partition = 0;
                    final Struct key = schemaChangeRecordKey(event);
                    final Struct value = schemaChangeRecordValue(event);
                    final SourceRecord record =
                            new SourceRecord(
                                    event.getPartition(),
                                    event.getOffset(),
                                    topicName,
                                    partition,
                                    schemaChangeKeySchema,
                                    key,
                                    schemaChangeValueSchema,
                                    value);
                    queue.enqueue(new DataChangeEvent(record));
                } catch (IOException e) {
                    throw new IllegalStateException(
                            String.format("dispatch schema change event %s error ", event), e);
                }
            }
        }
    }

    public void dispatchWatermarkEvent(
            Map<String, ?> sourcePartition,
            SourceSplitBase sourceSplit,
            Offset watermark,
            WatermarkKind watermarkKind)
            throws InterruptedException {

        SourceRecord sourceRecord =
                WatermarkEvent.create(
                        sourcePartition, topic, sourceSplit.splitId(), watermarkKind, watermark);
        queue.enqueue(new DataChangeEvent(sourceRecord));
    }
}
