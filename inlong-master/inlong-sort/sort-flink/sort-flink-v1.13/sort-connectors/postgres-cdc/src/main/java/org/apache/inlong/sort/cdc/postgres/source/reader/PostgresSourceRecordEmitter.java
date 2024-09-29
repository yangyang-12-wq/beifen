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

package org.apache.inlong.sort.cdc.postgres.source.reader;

import org.apache.inlong.sort.cdc.base.debezium.DebeziumDeserializationSchema;
import org.apache.inlong.sort.cdc.base.debezium.history.FlinkJsonTableChangeSerializer;
import org.apache.inlong.sort.cdc.base.source.meta.offset.Offset;
import org.apache.inlong.sort.cdc.base.source.meta.offset.OffsetFactory;
import org.apache.inlong.sort.cdc.base.source.meta.split.SourceSplitState;
import org.apache.inlong.sort.cdc.base.source.metrics.SourceReaderMetrics;
import org.apache.inlong.sort.cdc.base.source.reader.IncrementalSourceReader;
import org.apache.inlong.sort.cdc.base.source.reader.IncrementalSourceRecordEmitter;
import org.apache.inlong.sort.cdc.base.util.RecordUtils;

import io.debezium.connector.AbstractSourceInfo;
import io.debezium.data.Envelope;
import io.debezium.document.Array;
import io.debezium.relational.TableId;
import io.debezium.relational.history.HistoryRecord;
import io.debezium.relational.history.TableChanges;
import io.debezium.relational.history.TableChanges.TableChange;
import org.apache.flink.api.connector.source.SourceOutput;
import org.apache.flink.connector.base.source.reader.RecordEmitter;
import org.apache.flink.util.Collector;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static com.ververica.cdc.connectors.base.source.meta.wartermark.WatermarkEvent.isHighWatermarkEvent;
import static com.ververica.cdc.connectors.base.source.meta.wartermark.WatermarkEvent.isWatermarkEvent;
import static com.ververica.cdc.connectors.base.utils.SourceRecordUtils.getHistoryRecord;
import static com.ververica.cdc.connectors.base.utils.SourceRecordUtils.isDataChangeRecord;
import static com.ververica.cdc.connectors.base.utils.SourceRecordUtils.isSchemaChangeEvent;
import static org.apache.inlong.sort.cdc.base.util.RecordUtils.isHeartbeatEvent;

/**
 * The {@link RecordEmitter} implementation for {@link IncrementalSourceReader}.
 *
 * <p>The {@link RecordEmitter} buffers the snapshot records of split and call the stream reader to
 * emit records rather than emit the records directly.
 * Copy from com.ververica:flink-cdc-base:2.3.0.
 */
public class PostgresSourceRecordEmitter<T>
        extends
            IncrementalSourceRecordEmitter<T> {

    private static final Logger LOG = LoggerFactory.getLogger(PostgresSourceRecordEmitter.class);
    private static final FlinkJsonTableChangeSerializer TABLE_CHANGE_SERIALIZER =
            new FlinkJsonTableChangeSerializer();

    public PostgresSourceRecordEmitter(
            DebeziumDeserializationSchema<T> debeziumDeserializationSchema,
            SourceReaderMetrics sourceReaderMetrics,
            boolean includeSchemaChanges,
            OffsetFactory offsetFactory) {
        super(debeziumDeserializationSchema, sourceReaderMetrics, includeSchemaChanges, offsetFactory);
    }

    @Override
    protected void processElement(
            SourceRecord element, SourceOutput<T> output, SourceSplitState splitState)
            throws Exception {
        if (isWatermarkEvent(element)) {
            LOG.debug("PostgresSourceRecordEmitter Process WatermarkEvent: {}; splitState = {}", element, splitState);
            Offset watermark = super.getOffsetPosition(element);
            if (isHighWatermarkEvent(element) && splitState.isSnapshotSplitState()) {
                LOG.info("PostgresSourceRecordEmitter Set HighWatermark {} for {}", watermark, splitState);
                splitState.asSnapshotSplitState().setHighWatermark(watermark);
            }
        } else if (isSchemaChangeEvent(element) && splitState.isStreamSplitState()) {
            LOG.debug("PostgresSourceRecordEmitter Process SchemaChangeEvent: {}; splitState = {}", element,
                    splitState);
            HistoryRecord historyRecord = getHistoryRecord(element);
            Array tableChanges =
                    historyRecord.document().getArray(HistoryRecord.Fields.TABLE_CHANGES);
            TableChanges changes = TABLE_CHANGE_SERIALIZER.deserialize(tableChanges, true);
            for (TableChanges.TableChange tableChange : changes) {
                splitState.asStreamSplitState().recordSchema(tableChange.getId(), tableChange);
            }
            if (includeSchemaChanges) {
                emitElement(element, output);
            }
        } else if (isDataChangeRecord(element)) {
            LOG.debug("PostgresSourceRecordEmitter Process DataChangeRecord: {}; splitState = {}", element, splitState);
            updateStartingOffsetForSplit(splitState, element);
            reportMetrics(element);
            final Map<TableId, TableChange> tableSchemas =
                    splitState.getSourceSplitBase().getTableSchemas();
            final TableChange tableSchema =
                    tableSchemas.getOrDefault(RecordUtils.getTableId(element), null);
            debeziumDeserializationSchema.deserialize(element, new Collector<T>() {

                @Override
                public void collect(T record) {
                    Struct value = (Struct) element.value();
                    Struct source = value.getStruct(Envelope.FieldName.SOURCE);
                    String dbName = source.getString(AbstractSourceInfo.DATABASE_NAME_KEY);
                    String schemaName = source.getString(AbstractSourceInfo.SCHEMA_NAME_KEY);
                    String tableName = source.getString(AbstractSourceInfo.TABLE_NAME_KEY);
                    sourceReaderMetrics
                            .outputMetrics(dbName, schemaName, tableName, splitState.isSnapshotSplitState(), value);
                    output.collect(record);
                }

                @Override
                public void close() {

                }
            }, tableSchema);
        } else if (isHeartbeatEvent(element)) {
            LOG.debug("PostgresSourceRecordEmitterProcess Heartbeat: {}; splitState = {}", element, splitState);
            updateStartingOffsetForSplit(splitState, element);
        } else {
            // unknown element
            LOG.info(
                    "Meet unknown element {} for splitState = {}, just skip.", element, splitState);
        }
    }

}
