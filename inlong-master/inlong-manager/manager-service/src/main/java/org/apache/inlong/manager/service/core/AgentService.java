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

package org.apache.inlong.manager.service.core;

import org.apache.inlong.common.pojo.agent.AgentConfigInfo;
import org.apache.inlong.common.pojo.agent.AgentConfigRequest;
import org.apache.inlong.common.pojo.agent.TaskRequest;
import org.apache.inlong.common.pojo.agent.TaskResult;
import org.apache.inlong.common.pojo.agent.TaskSnapshotRequest;
import org.apache.inlong.common.pojo.agent.installer.ConfigRequest;
import org.apache.inlong.common.pojo.agent.installer.ConfigResult;
import org.apache.inlong.manager.pojo.cluster.agent.AgentClusterNodeBindGroupRequest;

/**
 * The service interface for agent
 */
public interface AgentService {

    /**
     * Report the heartbeat for given source.
     *
     * @param request Heartbeat request.
     * @return Whether succeed.
     */
    Boolean reportSnapshot(TaskSnapshotRequest request);

    /**
     * Agent report the task result.
     *
     * @param request Result of the task.
     */
    void report(TaskRequest request);

    /**
     * Agent cluster config.
     *
     * @param request Request of the agent config.
     * @return Agent config info result.
     */
    AgentConfigInfo getAgentConfig(AgentConfigRequest request);

    /**
     * Agent pull task config.
     *
     * @param request Request of the task.
     * @return Task result.
     */
    TaskResult getTaskResult(TaskRequest request);

    TaskResult getExistTaskConfig(TaskRequest request);

    /**
     * Divide the agent into different groups, which collect different stream source tasks.
     *
     * @param request Request of the bind group.
     * @return Whether succeed.
     */
    Boolean bindGroup(AgentClusterNodeBindGroupRequest request);

    ConfigResult getConfig(ConfigRequest request);

}
