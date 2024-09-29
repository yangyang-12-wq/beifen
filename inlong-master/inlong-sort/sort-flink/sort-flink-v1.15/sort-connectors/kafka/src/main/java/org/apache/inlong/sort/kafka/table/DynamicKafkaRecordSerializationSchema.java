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

package org.apache.inlong.sort.kafka.table;

import org.apache.inlong.sort.base.metric.MetricOption;
import org.apache.inlong.sort.base.metric.SinkMetricData;

import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.streaming.connectors.kafka.partitioner.FlinkKafkaPartitioner;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.types.RowKind;
import org.apache.flink.util.Preconditions;
import org.apache.kafka.clients.producer.ProducerRecord;

import javax.annotation.Nullable;

import static org.apache.flink.util.Preconditions.checkNotNull;
import static org.apache.inlong.sort.base.util.CalculateObjectSizeUtils.getDataSize;

/** SerializationSchema used by {@link KafkaDynamicSink} to configure a {KafkaSink}.
 * <p>
 * Copy from org.apache.flink:flink-connector-kafka:1.15.4
 * */
class DynamicKafkaRecordSerializationSchema implements KafkaRecordSerializationSchema<RowData> {

    private final String topic;
    private final FlinkKafkaPartitioner<RowData> partitioner;
    @Nullable
    private final SerializationSchema<RowData> keySerialization;
    private final SerializationSchema<RowData> valueSerialization;
    private final RowData.FieldGetter[] keyFieldGetters;
    private final RowData.FieldGetter[] valueFieldGetters;
    private final boolean hasMetadata;
    private final int[] metadataPositions;
    private final boolean upsertMode;
    private final MetricOption metricOption;
    private SinkMetricData sinkMetricData;

    DynamicKafkaRecordSerializationSchema(
            String topic,
            @Nullable FlinkKafkaPartitioner<RowData> partitioner,
            @Nullable SerializationSchema<RowData> keySerialization,
            SerializationSchema<RowData> valueSerialization,
            RowData.FieldGetter[] keyFieldGetters,
            RowData.FieldGetter[] valueFieldGetters,
            boolean hasMetadata,
            int[] metadataPositions,
            boolean upsertMode,
            MetricOption metricOption) {
        if (upsertMode) {
            Preconditions.checkArgument(
                    keySerialization != null && keyFieldGetters.length > 0,
                    "Key must be set in upsert mode for serialization schema.");
        }
        this.topic = checkNotNull(topic);
        this.partitioner = partitioner;
        this.keySerialization = keySerialization;
        this.valueSerialization = checkNotNull(valueSerialization);
        this.keyFieldGetters = keyFieldGetters;
        this.valueFieldGetters = valueFieldGetters;
        this.hasMetadata = hasMetadata;
        this.metadataPositions = metadataPositions;
        this.upsertMode = upsertMode;
        this.metricOption = metricOption;

    }

    @Override
    public ProducerRecord<byte[], byte[]> serialize(
            RowData consumedRow, KafkaSinkContext context, Long timestamp) {
        // shortcut in case no input projection is required
        if (keySerialization == null && !hasMetadata) {
            final byte[] valueSerialized = valueSerialization.serialize(consumedRow);
            ProducerRecord<byte[], byte[]> record = new ProducerRecord<>(
                    topic,
                    extractPartition(
                            consumedRow,
                            null,
                            valueSerialized,
                            context.getPartitionsForTopic(topic)),
                    null,
                    valueSerialized);
            if (sinkMetricData != null) {
                sinkMetricData.invoke(1, getDataSize(record));
            }
            return record;
        }
        final byte[] keySerialized;
        if (keySerialization == null) {
            keySerialized = null;
        } else {
            final RowData keyRow = createProjectedRow(consumedRow, RowKind.INSERT, keyFieldGetters);
            keySerialized = keySerialization.serialize(keyRow);
        }

        final byte[] valueSerialized;
        final RowKind kind = consumedRow.getRowKind();
        if (upsertMode) {
            if (kind == RowKind.DELETE || kind == RowKind.UPDATE_BEFORE) {
                // transform the message as the tombstone message
                valueSerialized = null;
            } else {
                // make the message to be INSERT to be compliant with the INSERT-ONLY format
                final RowData valueRow =
                        DynamicKafkaRecordSerializationSchema.createProjectedRow(
                                consumedRow, kind, valueFieldGetters);
                valueRow.setRowKind(RowKind.INSERT);
                valueSerialized = valueSerialization.serialize(valueRow);
            }
        } else {
            final RowData valueRow =
                    DynamicKafkaRecordSerializationSchema.createProjectedRow(
                            consumedRow, kind, valueFieldGetters);
            valueSerialized = valueSerialization.serialize(valueRow);
        }

        ProducerRecord<byte[], byte[]> record = new ProducerRecord<>(
                topic,
                extractPartition(
                        consumedRow,
                        keySerialized,
                        valueSerialized,
                        context.getPartitionsForTopic(topic)),
                readMetadata(consumedRow, KafkaDynamicSink.WritableMetadata.TIMESTAMP),
                keySerialized,
                valueSerialized,
                readMetadata(consumedRow, KafkaDynamicSink.WritableMetadata.HEADERS));
        if (sinkMetricData != null) {
            sinkMetricData.invoke(1, getDataSize(record));
        }
        return record;
    }

    @Override
    public void open(
            SerializationSchema.InitializationContext context, KafkaSinkContext sinkContext)
            throws Exception {
        if (keySerialization != null) {
            keySerialization.open(context);
        }
        if (partitioner != null) {
            partitioner.open(
                    sinkContext.getParallelInstanceId(),
                    sinkContext.getNumberOfParallelInstances());
        }
        valueSerialization.open(context);

        if (metricOption != null) {
            this.sinkMetricData = new SinkMetricData(metricOption, context.getMetricGroup());
        }
    }

    private Integer extractPartition(
            RowData consumedRow,
            @Nullable byte[] keySerialized,
            byte[] valueSerialized,
            int[] partitions) {
        if (partitioner != null) {
            return partitioner.partition(
                    consumedRow, keySerialized, valueSerialized, topic, partitions);
        }
        return null;
    }

    static RowData createProjectedRow(
            RowData consumedRow, RowKind kind, RowData.FieldGetter[] fieldGetters) {
        final int arity = fieldGetters.length;
        final GenericRowData genericRowData = new GenericRowData(kind, arity);
        for (int fieldPos = 0; fieldPos < arity; fieldPos++) {
            genericRowData.setField(fieldPos, fieldGetters[fieldPos].getFieldOrNull(consumedRow));
        }
        return genericRowData;
    }

    @SuppressWarnings("unchecked")
    private <T> T readMetadata(RowData consumedRow, KafkaDynamicSink.WritableMetadata metadata) {
        final int pos = metadataPositions[metadata.ordinal()];
        if (pos < 0) {
            return null;
        }
        return (T) metadata.converter.read(consumedRow, pos);
    }
}
