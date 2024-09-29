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

package org.apache.inlong.audit.entities;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Source config
 */
@Data
@AllArgsConstructor
public class SourceConfig {

    private AuditCycle auditCycle;
    private String querySql;
    private int statBackTimes;
    private final String driverClassName;
    private final String jdbcUrl;
    private final String username;
    private final String password;
    private boolean needJoin = false;

    public SourceConfig(AuditCycle auditCycle,
            String querySql,
            int statBackTimes,
            String driverClassName,
            String jdbcUrl,
            String username,
            String password) {
        this.auditCycle = auditCycle;
        this.querySql = querySql;
        this.statBackTimes = statBackTimes;
        this.driverClassName = driverClassName;
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }
}
