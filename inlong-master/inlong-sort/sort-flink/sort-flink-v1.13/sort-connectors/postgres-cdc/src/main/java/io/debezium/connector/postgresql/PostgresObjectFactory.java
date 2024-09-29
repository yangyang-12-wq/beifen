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

package io.debezium.connector.postgresql;

import io.debezium.connector.postgresql.connection.PostgresConnection;
import io.debezium.connector.postgresql.connection.ReplicationConnection;
import io.debezium.relational.TableId;
import io.debezium.schema.TopicSelector;
import io.debezium.util.Clock;
import io.debezium.util.Metronome;
import org.apache.flink.util.FlinkRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Duration;
import java.time.ZoneOffset;

/**
 * A factory for creating various Debezium objects
 *
 * <p>It is a hack to access package-private constructor in debezium.
 */
public class PostgresObjectFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresObjectFactory.class);

    /** Create a new PostgresSchema and initialize the content of the schema. */
    public static PostgresSchema newSchema(
            PostgresConnection connection,
            PostgresConnectorConfig config,
            TypeRegistry typeRegistry,
            TopicSelector<TableId> topicSelector,
            PostgresValueConverter valueConverter)
            throws SQLException {
        PostgresSchema schema =
                new PostgresSchema(config, typeRegistry, topicSelector, valueConverter);
        schema.refresh(connection, false);
        return schema;
    }

    public static PostgresTaskContext newTaskContext(
            PostgresConnectorConfig connectorConfig,
            PostgresSchema schema,
            TopicSelector<TableId> topicSelector) {
        return new PostgresTaskContext(connectorConfig, schema, topicSelector);
    }

    public static PostgresEventMetadataProvider newEventMetadataProvider() {
        return new PostgresEventMetadataProvider();
    }

    /**
     * Create a new PostgresVauleConverterBuilder instance and offer type registry for JDBC
     * connection.
     *
     * <p>It is created in this package because some methods (e.g., includeUnknownDatatypes) of
     * PostgresConnectorConfig is protected.
     */
    public static PostgresConnection.PostgresValueConverterBuilder newPostgresValueConverterBuilder(
            PostgresConnectorConfig config) {
        return typeRegistry -> new PostgresValueConverter(
                StandardCharsets.UTF_8,
                config.getDecimalMode(),
                config.getTemporalPrecisionMode(),
                ZoneOffset.UTC,
                null,
                config.includeUnknownDatatypes(),
                typeRegistry,
                config.hStoreHandlingMode(),
                config.binaryHandlingMode(),
                config.intervalHandlingMode(),
                config.toastedValuePlaceholder());
    }

    // modified from
    // io.debezium.connector.postgresql.PostgresConnectorTask.createReplicationConnection
    public static ReplicationConnection createReplicationConnection(
            PostgresTaskContext taskContext,
            boolean doSnapshot,
            PostgresConnectorConfig connectorConfig) {
        int maxRetries = connectorConfig.maxRetries();
        Duration retryDelay = connectorConfig.retryDelay();

        final Metronome metronome = Metronome.parker(retryDelay, Clock.SYSTEM);
        short retryCount = 0;
        while (retryCount <= maxRetries) {
            try {
                LOGGER.info("Creating a new replication connection for {}", taskContext);
                return taskContext.createReplicationConnection(doSnapshot);
            } catch (SQLException ex) {
                retryCount++;
                if (retryCount > maxRetries) {
                    LOGGER.error(
                            "Too many errors connecting to server. All {} retries failed.",
                            maxRetries);
                    throw new FlinkRuntimeException(ex);
                }

                LOGGER.warn(
                        "Error connecting to server; will attempt retry {} of {} after {} "
                                + "seconds. Exception message: {}",
                        retryCount,
                        maxRetries,
                        retryDelay.getSeconds(),
                        ex.getMessage());
                try {
                    metronome.pause();
                } catch (InterruptedException e) {
                    LOGGER.warn("Connection retry sleep interrupted by exception: " + e);
                    Thread.currentThread().interrupt();
                }
            }
        }
        LOGGER.error("Failed to create replication connection after {} retries", maxRetries);
        throw new FlinkRuntimeException(
                "Failed to create replication connection for " + taskContext);
    }
}
