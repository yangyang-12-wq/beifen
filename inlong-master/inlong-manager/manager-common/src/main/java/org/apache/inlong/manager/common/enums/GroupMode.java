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

package org.apache.inlong.manager.common.enums;

import lombok.Getter;

/**
 * Mode of inlong group
 */
public enum GroupMode {

    /**
     * Standard mode(include Data Ingestion and Synchronization): group init with all components in InLong Cluster
     * StreamSource -> Agent/SDK -> DataProxy -> MQ Cache -> Sort -> StreamSink
     */
    STANDARD("standard"),

    /**
     * DataSync mode(only Data Synchronization): real-time data sync in stream way, group init only with
     * sort in InLong Cluster.
     * StreamSource -> Sort -> Sink
     */
    DATASYNC("datasync"),

    /**
     * DataSync mode(only Data Synchronization): offline data sync in batch way, group init only with sort
     * in InLong Cluster.
     * BatchSource -> Sort -> Sink
     */
    DATASYNC_BATCH("datasync_offline");

    @Getter
    private final String mode;

    GroupMode(String mode) {
        this.mode = mode;
    }

    public static GroupMode forMode(String mode) {
        for (GroupMode groupMode : values()) {
            if (groupMode.getMode().equals(mode)) {
                return groupMode;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported group mode for %s", mode));
    }

}
