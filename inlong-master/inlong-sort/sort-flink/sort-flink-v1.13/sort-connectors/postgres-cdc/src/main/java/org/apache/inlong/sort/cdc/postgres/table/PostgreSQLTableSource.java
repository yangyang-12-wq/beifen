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

package org.apache.inlong.sort.cdc.postgres.table;

import org.apache.inlong.sort.base.filter.RowKindValidator;
import org.apache.inlong.sort.cdc.base.debezium.DebeziumDeserializationSchema;
import org.apache.inlong.sort.cdc.base.debezium.table.MetadataConverter;
import org.apache.inlong.sort.cdc.base.debezium.table.RowDataDebeziumDeserializeSchema;
import org.apache.inlong.sort.cdc.postgres.DebeziumSourceFunction;
import org.apache.inlong.sort.cdc.postgres.PostgreSQLSource;
import org.apache.inlong.sort.cdc.postgres.source.PostgresSourceBuilder;

import com.ververica.cdc.connectors.base.options.StartupOptions;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.table.catalog.ResolvedSchema;
import org.apache.flink.table.connector.ChangelogMode;
import org.apache.flink.table.connector.source.DynamicTableSource;
import org.apache.flink.table.connector.source.ScanTableSource;
import org.apache.flink.table.connector.source.SourceFunctionProvider;
import org.apache.flink.table.connector.source.SourceProvider;
import org.apache.flink.table.connector.source.abilities.SupportsReadingMetadata;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.types.RowKind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import java.time.Duration;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.flink.util.Preconditions.checkNotNull;

/**
 * A {@link DynamicTableSource} that describes how to create a PostgreSQL source from a logical
 * description.
 */
public class PostgreSQLTableSource implements ScanTableSource, SupportsReadingMetadata {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostgreSQLTableSource.class);

    private final ResolvedSchema physicalSchema;
    private final int port;
    private final String hostname;
    private final String database;
    private final String schemaName;
    private final String tableName;
    private final String username;
    private final String password;
    private final String pluginName;
    private final String slotName;
    private final String serverTimeZone;
    private final Properties dbzProperties;
    private final boolean sourceMultipleEnable;
    private final String inlongMetric;
    private final String inlongAudit;
    private final boolean appendSource;
    private final String rowKindFiltered;
    private final boolean enableParallelRead;
    private final int splitSize;
    private final int splitMetaGroupSize;
    private final int fetchSize;
    private final Duration connectTimeout;
    private final int connectionPoolSize;
    private final int connectMaxRetries;
    private final double distributionFactorUpper;
    private final double distributionFactorLower;
    private final Duration heartbeatInterval;
    private final StartupOptions startupOptions;
    private final String chunkKeyColumn;

    // --------------------------------------------------------------------------------------------
    // Mutable attributes
    // --------------------------------------------------------------------------------------------

    /**
     * Data type that describes the final output of the source.
     */
    protected DataType producedDataType;

    /**
     * Metadata that is appended at the end of a physical source row.
     */
    protected List<String> metadataKeys;

    public PostgreSQLTableSource(
            ResolvedSchema physicalSchema,
            int port,
            String hostname,
            String database,
            String schemaName,
            String tableName,
            String username,
            String password,
            String pluginName,
            String slotName,
            String serverTimeZone,
            Properties dbzProperties,
            boolean appendSource,
            String rowKindFiltered,
            boolean sourceMultipleEnable,
            String inlongMetric,
            String inlongAudit,
            boolean enableParallelRead,
            int splitSize,
            int splitMetaGroupSize,
            int fetchSize,
            Duration connectTimeout,
            int connectMaxRetries,
            int connectionPoolSize,
            double distributionFactorUpper,
            double distributionFactorLower,
            Duration heartbeatInterval,
            StartupOptions startupOptions,
            @Nullable String chunkKeyColumn) {
        this.physicalSchema = physicalSchema;
        this.port = port;
        this.hostname = checkNotNull(hostname);
        this.database = checkNotNull(database);
        this.schemaName = checkNotNull(schemaName);
        this.tableName = checkNotNull(tableName);
        this.username = checkNotNull(username);
        this.password = checkNotNull(password);
        this.pluginName = checkNotNull(pluginName);
        this.slotName = slotName;
        this.serverTimeZone = serverTimeZone;
        this.dbzProperties = dbzProperties;
        this.producedDataType = physicalSchema.toPhysicalRowDataType();
        this.metadataKeys = Collections.emptyList();
        this.appendSource = appendSource;
        this.rowKindFiltered = rowKindFiltered;
        this.sourceMultipleEnable = sourceMultipleEnable;
        this.inlongMetric = inlongMetric;
        this.inlongAudit = inlongAudit;
        this.enableParallelRead = enableParallelRead;
        this.splitSize = splitSize;
        this.splitMetaGroupSize = splitMetaGroupSize;
        this.fetchSize = fetchSize;
        this.connectTimeout = connectTimeout;
        this.connectMaxRetries = connectMaxRetries;
        this.connectionPoolSize = connectionPoolSize;
        this.distributionFactorUpper = distributionFactorUpper;
        this.distributionFactorLower = distributionFactorLower;
        this.heartbeatInterval = heartbeatInterval;
        this.startupOptions = startupOptions;
        this.chunkKeyColumn = chunkKeyColumn;
    }

    @Override
    public ChangelogMode getChangelogMode() {
        final ChangelogMode.Builder builder =
                ChangelogMode.newBuilder().addContainedKind(RowKind.INSERT);
        if (!appendSource) {
            builder.addContainedKind(RowKind.UPDATE_BEFORE)
                    .addContainedKind(RowKind.UPDATE_AFTER)
                    .addContainedKind(RowKind.DELETE);
        }
        return builder.build();
    }

    @Override
    public ScanRuntimeProvider getScanRuntimeProvider(ScanContext scanContext) {
        RowType physicalDataType =
                (RowType) physicalSchema.toPhysicalRowDataType().getLogicalType();
        MetadataConverter[] metadataConverters = getMetadataConverters();
        TypeInformation<RowData> typeInfo = scanContext.createTypeInformation(producedDataType);
        DebeziumDeserializationSchema<RowData> deserializer =
                RowDataDebeziumDeserializeSchema.newBuilder()
                        .setPhysicalRowType(physicalDataType)
                        .setMetadataConverters(metadataConverters)
                        .setResultTypeInfo(typeInfo)
                        .setUserDefinedConverterFactory(
                                PostgreSQLDeserializationConverterFactory.instance())
                        .setMigrateAll(sourceMultipleEnable)
                        .setServerTimeZone(ZoneId.of(serverTimeZone))
                        .setAppendSource(appendSource)
                        .setValidator(new RowKindValidator(rowKindFiltered))
                        .build();

        if (enableParallelRead) {
            LOGGER.info("in  PostgreSQLTableSource, enableParallelRead is true");
            PostgresSourceBuilder.PostgresIncrementalSource<RowData> parallelSource =
                    new PostgresSourceBuilder<RowData>()
                            .hostname(hostname)
                            .port(port)
                            .database(database)
                            .tableList(tableName)
                            .username(username)
                            .password(password)
                            .inlongMetric(inlongMetric)
                            .inlongAudit(inlongAudit)
                            .decodingPluginName(pluginName)
                            .slotName(slotName)
                            .debeziumProperties(dbzProperties)
                            .deserializer(deserializer)
                            .splitSize(splitSize)
                            .splitMetaGroupSize(splitMetaGroupSize)
                            .distributionFactorUpper(distributionFactorUpper)
                            .distributionFactorLower(distributionFactorLower)
                            .fetchSize(fetchSize)
                            .connectTimeout(connectTimeout)
                            .connectMaxRetries(connectMaxRetries)
                            .connectionPoolSize(connectionPoolSize)
                            .startupOptions(startupOptions)
                            .chunkKeyColumn(chunkKeyColumn)
                            .heartbeatInterval(heartbeatInterval)
                            .build();
            return SourceProvider.of(parallelSource);
        } else {
            LOGGER.info("in  PostgreSQLTableSource, enableParallelRead is false");
            DebeziumSourceFunction<RowData> sourceFunction =
                    PostgreSQLSource.<RowData>builder()
                            .hostname(hostname)
                            .port(port)
                            .database(database)
                            .schemaList(schemaName.split(","))
                            .tableList(tableName)
                            .username(username)
                            .password(password)
                            .decodingPluginName(pluginName)
                            .slotName(slotName)
                            .debeziumProperties(dbzProperties)
                            .deserializer(deserializer)
                            .inlongMetric(inlongMetric)
                            .inlongAudit(inlongAudit)
                            .migrateAll(sourceMultipleEnable)
                            .build();
            return SourceFunctionProvider.of(sourceFunction, false);
        }
    }

    private MetadataConverter[] getMetadataConverters() {
        if (metadataKeys.isEmpty()) {
            return new MetadataConverter[0];
        }

        return metadataKeys.stream()
                .map(key -> Stream.of(PostgreSQLReadableMetaData.values())
                        .filter(m -> m.getKey().equals(key))
                        .findFirst()
                        .orElseThrow(IllegalStateException::new))
                .map(PostgreSQLReadableMetaData::getConverter)
                .toArray(MetadataConverter[]::new);
    }

    @Override
    public DynamicTableSource copy() {
        PostgreSQLTableSource source =
                new PostgreSQLTableSource(
                        physicalSchema,
                        port,
                        hostname,
                        database,
                        schemaName,
                        tableName,
                        username,
                        password,
                        pluginName,
                        slotName,
                        serverTimeZone,
                        dbzProperties,
                        appendSource,
                        rowKindFiltered,
                        sourceMultipleEnable,
                        inlongMetric,
                        inlongAudit,
                        enableParallelRead,
                        splitSize,
                        splitMetaGroupSize,
                        fetchSize,
                        connectTimeout,
                        connectMaxRetries,
                        connectionPoolSize,
                        distributionFactorUpper,
                        distributionFactorLower,
                        heartbeatInterval,
                        startupOptions,
                        chunkKeyColumn);
        source.metadataKeys = metadataKeys;
        source.producedDataType = producedDataType;
        return source;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PostgreSQLTableSource that = (PostgreSQLTableSource) o;
        return port == that.port
                && Objects.equals(physicalSchema, that.physicalSchema)
                && Objects.equals(hostname, that.hostname)
                && Objects.equals(database, that.database)
                && Objects.equals(schemaName, that.schemaName)
                && Objects.equals(tableName, that.tableName)
                && Objects.equals(username, that.username)
                && Objects.equals(password, that.password)
                && Objects.equals(pluginName, that.pluginName)
                && Objects.equals(slotName, that.slotName)
                && Objects.equals(serverTimeZone, that.serverTimeZone)
                && Objects.equals(dbzProperties, that.dbzProperties)
                && Objects.equals(producedDataType, that.producedDataType)
                && Objects.equals(metadataKeys, that.metadataKeys)
                && Objects.equals(inlongMetric, that.inlongMetric)
                && Objects.equals(inlongAudit, that.inlongAudit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                physicalSchema,
                port,
                hostname,
                database,
                schemaName,
                tableName,
                username,
                password,
                pluginName,
                slotName,
                serverTimeZone,
                dbzProperties,
                producedDataType,
                metadataKeys,
                inlongMetric,
                inlongAudit);
    }

    @Override
    public String asSummaryString() {
        return "PostgreSQL-CDC";
    }

    @Override
    public Map<String, DataType> listReadableMetadata() {
        return Stream.of(PostgreSQLReadableMetaData.values())
                .collect(
                        Collectors.toMap(
                                PostgreSQLReadableMetaData::getKey,
                                PostgreSQLReadableMetaData::getDataType));
    }

    @Override
    public void applyReadableMetadata(List<String> metadataKeys, DataType producedDataType) {
        this.metadataKeys = metadataKeys;
        this.producedDataType = producedDataType;
    }
}
