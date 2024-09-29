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

package org.apache.inlong.manager.pojo.sink.kudu;

import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.JsonUtils;
import org.apache.inlong.manager.pojo.sink.BaseStreamSink;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Kudu sink info
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KuduSinkDTO extends BaseStreamSink {

    @ApiModelProperty("Kudu masters, a comma separated list of 'host:port' pairs")
    private String masters;

    @ApiModelProperty("Target table name")
    private String tableName;

    @ApiModelProperty("Properties for Kudu")
    private Map<String, Object> properties;

    @ApiModelProperty("Partition field list")
    private String partitionKey;

    @ApiModelProperty("Buckets for the newly created table")
    private Integer buckets;

    /**
     * Get the dto instance from the request
     */
    public static KuduSinkDTO getFromRequest(KuduSinkRequest request, String extParams) {
        KuduSinkDTO dto = StringUtils.isNotBlank(extParams) ? KuduSinkDTO.getFromJson(extParams) : new KuduSinkDTO();
        return CommonBeanUtils.copyProperties(request, dto, true);
    }

    public static KuduSinkDTO getFromJson(@NotNull String extParams) {
        try {
            return JsonUtils.parseObject(extParams, KuduSinkDTO.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SINK_INFO_INCORRECT,
                    String.format("parse extParams of Kudu SinkDTO failure: %s", e.getMessage()));
        }
    }

    /**
     * Get Kudu table info
     */
    public static KuduTableInfo getKuduTableInfo(KuduSinkDTO kuduInfo, List<KuduColumnInfo> columnList) {
        KuduTableInfo tableInfo = new KuduTableInfo();
        tableInfo.setTableName(kuduInfo.getTableName());
        tableInfo.setMasters(kuduInfo.getMasters());
        tableInfo.setColumns(columnList);
        tableInfo.setTblProperties(kuduInfo.getProperties());
        tableInfo.setBuckets(kuduInfo.getBuckets());
        return tableInfo;
    }

}
