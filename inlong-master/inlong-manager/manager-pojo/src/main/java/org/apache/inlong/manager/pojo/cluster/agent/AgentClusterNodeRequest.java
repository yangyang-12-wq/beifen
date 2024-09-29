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

package org.apache.inlong.manager.pojo.cluster.agent;

import org.apache.inlong.manager.common.enums.ClusterType;
import org.apache.inlong.manager.common.util.JsonTypeDefine;
import org.apache.inlong.manager.pojo.cluster.ClusterNodeRequest;
import org.apache.inlong.manager.pojo.module.ModuleHistory;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * Inlong cluster node request for Agent
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@JsonTypeDefine(value = ClusterType.AGENT)
@ApiModel("Inlong cluster node request for Agent")
public class AgentClusterNodeRequest extends ClusterNodeRequest {

    @ApiModelProperty(value = "Agent group name")
    private String agentGroup;

    @ApiModelProperty(value = "Agent restart time")
    private Integer agentRestartTime = 0;

    @ApiModelProperty(value = "Install restart time")
    private Integer installRestartTime = 0;

    @ApiModelProperty(value = "Module id list")
    @Default
    private List<Integer> moduleIdList = new ArrayList<>();

    @ApiModelProperty("History list of module")
    @Default
    private List<ModuleHistory> moduleHistoryList = new ArrayList<>();

    public AgentClusterNodeRequest() {
        this.setType(ClusterType.AGENT);
    }

}
