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

package org.apache.inlong.manager.pojo.cluster;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import java.util.List;

/**
 * Inlong cluster bind or unbind tag request
 */
@Data
@ApiModel("Cluster bind and unbind tag request")
public class BindTagRequest {

    @ApiModelProperty(value = "Cluster tag")
    @NotBlank(message = "clusterTag cannot be blank")
    @Length(min = 1, max = 128, message = "length must be between 1 and 128")
    @Pattern(regexp = "^[a-z0-9_.-]{1,128}$", message = "only supports lowercase letters, numbers, '-', or '_'")
    private String clusterTag;

    @ApiModelProperty(value = "Cluster-ID list which needs to bind tag")
    private List<Integer> bindClusters;

    @ApiModelProperty(value = "Cluster-ID list which needs to unbind tag")
    private List<Integer> unbindClusters;

}
