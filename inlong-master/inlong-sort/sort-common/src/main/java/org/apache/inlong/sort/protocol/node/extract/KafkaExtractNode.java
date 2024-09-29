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

package org.apache.inlong.sort.protocol.node.extract;

import org.apache.inlong.common.enums.MetaField;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.InlongMetric;
import org.apache.inlong.sort.protocol.Metadata;
import org.apache.inlong.sort.protocol.constant.KafkaConstant;
import org.apache.inlong.sort.protocol.enums.KafkaScanStartupMode;
import org.apache.inlong.sort.protocol.node.ExtractNode;
import org.apache.inlong.sort.protocol.node.format.AvroFormat;
import org.apache.inlong.sort.protocol.node.format.CsvFormat;
import org.apache.inlong.sort.protocol.node.format.Format;
import org.apache.inlong.sort.protocol.node.format.InLongMsgFormat;
import org.apache.inlong.sort.protocol.node.format.JsonFormat;
import org.apache.inlong.sort.protocol.transformation.WatermarkField;

import com.google.common.base.Preconditions;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeName;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Kafka extract node for extract data from kafka
 */
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("kafkaExtract")
@JsonInclude(Include.NON_NULL)
@Data
public class KafkaExtractNode extends ExtractNode implements InlongMetric, Metadata, Serializable {

    private static final long serialVersionUID = 1L;

    @Nonnull
    @JsonProperty("topic")
    private String topic;
    @Nonnull
    @JsonProperty("bootstrapServers")
    private String bootstrapServers;
    @Nonnull
    @JsonProperty("format")
    private Format format;

    @JsonProperty("scanStartupMode")
    private KafkaScanStartupMode kafkaScanStartupMode;

    @JsonProperty("primaryKey")
    private String primaryKey;

    @JsonProperty("groupId")
    private String groupId;

    @JsonProperty("scanSpecificOffsets")
    private String scanSpecificOffsets;

    @JsonProperty("scanTimestampMillis")
    private String scanTimestampMillis;

    public KafkaExtractNode(@JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("fields") List<FieldInfo> fields,
            @Nullable @JsonProperty("watermarkField") WatermarkField watermarkField,
            @JsonProperty("properties") Map<String, String> properties,
            @Nonnull @JsonProperty("topic") String topic,
            @Nonnull @JsonProperty("bootstrapServers") String bootstrapServers,
            @Nonnull @JsonProperty("format") Format format,
            @JsonProperty("scanStartupMode") KafkaScanStartupMode kafkaScanStartupMode,
            @JsonProperty("primaryKey") String primaryKey,
            @JsonProperty("groupId") String groupId) {
        this(id, name, fields, watermarkField, properties, topic, bootstrapServers, format, kafkaScanStartupMode,
                primaryKey, groupId, null, null);
    }

    @JsonCreator
    public KafkaExtractNode(@JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("fields") List<FieldInfo> fields,
            @Nullable @JsonProperty("watermarkField") WatermarkField watermarkField,
            @JsonProperty("properties") Map<String, String> properties,
            @Nonnull @JsonProperty("topic") String topic,
            @Nonnull @JsonProperty("bootstrapServers") String bootstrapServers,
            @Nonnull @JsonProperty("format") Format format,
            @JsonProperty("scanStartupMode") KafkaScanStartupMode kafkaScanStartupMode,
            @JsonProperty("primaryKey") String primaryKey,
            @JsonProperty("groupId") String groupId,
            @JsonProperty("scanSpecificOffsets") String scanSpecificOffsets,
            @JsonProperty("scanTimestampMillis") String scanTimestampMillis) {
        super(id, name, fields, watermarkField, properties);
        this.topic = Preconditions.checkNotNull(topic, "kafka topic is empty");
        this.bootstrapServers = Preconditions.checkNotNull(bootstrapServers, "kafka bootstrapServers is empty");
        this.format = Preconditions.checkNotNull(format, "kafka format is empty");
        this.kafkaScanStartupMode = kafkaScanStartupMode;
        this.primaryKey = primaryKey;
        this.groupId = groupId;
        if (kafkaScanStartupMode == KafkaScanStartupMode.SPECIFIC_OFFSETS) {
            Preconditions.checkArgument(StringUtils.isNotEmpty(scanSpecificOffsets), "scanSpecificOffsets is empty");
            this.scanSpecificOffsets = scanSpecificOffsets;
        }
        if (KafkaScanStartupMode.TIMESTAMP_MILLIS == kafkaScanStartupMode) {
            Preconditions.checkArgument(StringUtils.isNotBlank(scanTimestampMillis), "scanTimestampMillis is empty");
            this.scanTimestampMillis = scanTimestampMillis;
        }
        if (KafkaScanStartupMode.GROUP_OFFSETS == kafkaScanStartupMode) {
            Preconditions.checkArgument(StringUtils.isNotBlank(groupId), "group is empty when enable group offsets");
        }
    }

    /**
     * Generate table options for Kafka extract node.
     * <p/>
     * Upsert Kafka stores message keys and values as bytes, so no need specified the schema or data types for Kafka.
     * <br/>
     * The messages of Kafka are serialized and deserialized by formats, e.g. csv, json, avro.
     * <br/>
     * Thus, the data type mapping is determined by specific formats.
     * <p/>
     * For more details:
     * <a href="https://nightlies.apache.org/flink/flink-docs-release-1.13/docs/connectors/table/upsert-kafka/">
     * upsert-kafka</a>
     *
     * @return options
     */
    @Override
    public Map<String, String> tableOptions() {
        Map<String, String> options = super.tableOptions();
        options.put(KafkaConstant.TOPIC, topic);
        options.put(KafkaConstant.PROPERTIES_BOOTSTRAP_SERVERS, bootstrapServers);
        if (isUpsertKafkaConnector(format, !StringUtils.isEmpty(this.primaryKey))) {
            options.put(KafkaConstant.CONNECTOR, KafkaConstant.UPSERT_KAFKA);
            options.putAll(format.generateOptions(true));
        } else {
            options.put(KafkaConstant.CONNECTOR, KafkaConstant.KAFKA);
            options.putAll(format.generateOptions(false));
            options.put(KafkaConstant.SCAN_STARTUP_MODE, kafkaScanStartupMode.getValue());
            if (StringUtils.isNotEmpty(scanSpecificOffsets)) {
                options.put(KafkaConstant.SCAN_STARTUP_SPECIFIC_OFFSETS, scanSpecificOffsets);
            }
            if (StringUtils.isNotBlank(scanTimestampMillis)) {
                options.put(KafkaConstant.SCAN_STARTUP_TIMESTAMP_MILLIS, scanTimestampMillis);
            }
        }
        if (StringUtils.isNotEmpty(groupId)) {
            options.put(KafkaConstant.PROPERTIES_GROUP_ID, groupId);
        }
        return options;
    }

    /**
     * true is upsert kafka connector
     * false is kafka connector
     * @return Boolean variable that decides connector option
     */
    private boolean isUpsertKafkaConnector(Format format, boolean hasPrimaryKey) {
        if (format instanceof JsonFormat && hasPrimaryKey) {
            return true;
        } else if (format instanceof CsvFormat && hasPrimaryKey) {
            return true;
        } else {
            return format instanceof AvroFormat && hasPrimaryKey;
        }
    }

    @Override
    public String genTableName() {
        return String.format("table_%s", super.getId());
    }

    @Override
    public String getPrimaryKey() {
        return primaryKey;
    }

    @Override
    public List<FieldInfo> getPartitionFields() {
        return super.getPartitionFields();
    }

    @Override
    public String getMetadataKey(MetaField metaField) {
        String metadataKey;
        switch (metaField) {
            case TABLE_NAME:
                metadataKey = "value.table";
                break;
            case DATABASE_NAME:
                metadataKey = "value.database";
                break;
            case SQL_TYPE:
                metadataKey = "value.sql-type";
                break;
            case PK_NAMES:
                metadataKey = "value.pk-names";
                break;
            case TS:
                metadataKey = "value.ingestion-timestamp";
                break;
            case OP_TS:
                metadataKey = "value.event-timestamp";
                break;
            case OP_TYPE:
                metadataKey = "value.type";
                break;
            case IS_DDL:
                metadataKey = "value.is-ddl";
                break;
            case MYSQL_TYPE:
                metadataKey = "value.mysql-type";
                break;
            case BATCH_ID:
                metadataKey = "value.batch-id";
                break;
            case UPDATE_BEFORE:
                metadataKey = "value.update-before";
                break;
            case KEY:
                metadataKey = "key";
                break;
            case VALUE:
                metadataKey = "value";
                break;
            case HEADERS:
                metadataKey = "headers";
                break;
            case HEADERS_TO_JSON_STR:
                metadataKey = "headers_to_json_str";
                break;
            case PARTITION:
                metadataKey = "partition";
                break;
            case OFFSET:
                metadataKey = "offset";
                break;
            case TIMESTAMP:
                metadataKey = "timestamp";
                break;
            case AUDIT_DATA_TIME:
                if (format instanceof InLongMsgFormat) {
                    metadataKey = INLONG_MSG_AUDIT_TIME;
                } else {
                    metadataKey = CONSUME_AUDIT_TIME;
                }
                break;
            default:
                throw new UnsupportedOperationException(String.format("Unsupport meta field for %s: %s",
                        this.getClass().getSimpleName(), metaField));
        }
        return metadataKey;
    }

    @Override
    public boolean isVirtual(MetaField metaField) {
        switch (metaField) {
            case KEY:
            case VALUE:
            case HEADERS:
            case HEADERS_TO_JSON_STR:
            case PARTITION:
            case OFFSET:
            case TIMESTAMP:
            case AUDIT_DATA_TIME:
                return true;
            default:
                return false;
        }
    }

    @Override
    public Set<MetaField> supportedMetaFields() {
        return EnumSet.of(MetaField.PROCESS_TIME, MetaField.TABLE_NAME, MetaField.OP_TYPE, MetaField.DATABASE_NAME,
                MetaField.SQL_TYPE, MetaField.PK_NAMES, MetaField.TS, MetaField.OP_TS, MetaField.IS_DDL,
                MetaField.MYSQL_TYPE, MetaField.BATCH_ID, MetaField.UPDATE_BEFORE,
                MetaField.KEY, MetaField.VALUE, MetaField.PARTITION, MetaField.HEADERS,
                MetaField.HEADERS_TO_JSON_STR, MetaField.OFFSET, MetaField.TIMESTAMP, MetaField.AUDIT_DATA_TIME);
    }
}
