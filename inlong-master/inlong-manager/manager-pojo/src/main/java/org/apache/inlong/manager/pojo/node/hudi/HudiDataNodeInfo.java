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

package org.apache.inlong.manager.pojo.node.hudi;

import org.apache.inlong.manager.common.consts.DataNodeType;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.JsonTypeDefine;
import org.apache.inlong.manager.pojo.node.DataNodeInfo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Hudi data node info
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@JsonTypeDefine(value = DataNodeType.HUDI)
@ApiModel("Hudi data node info")
public class HudiDataNodeInfo extends DataNodeInfo {

    @ApiModelProperty("Catalog type, like: HIVE, HADOOP, default is HIVE")
    private String catalogType = "HIVE";

    @ApiModelProperty("Hudi data warehouse dir")
    private String warehouse;

    public HudiDataNodeInfo() {
        this.setType(DataNodeType.HUDI);
    }

    @Override
    public HudiDataNodeRequest genRequest() {
        return CommonBeanUtils.copyProperties(this, HudiDataNodeRequest::new);
    }
}
