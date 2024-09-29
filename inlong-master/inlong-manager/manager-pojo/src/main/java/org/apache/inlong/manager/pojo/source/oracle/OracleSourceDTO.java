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

package org.apache.inlong.manager.pojo.source.oracle;

import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.JsonUtils;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.NotNull;

import java.util.Map;

/**
 * Oracle source info
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OracleSourceDTO {

    @ApiModelProperty("Hostname of the Oracle server")
    private String hostname;

    @ApiModelProperty("Port of the Oracle server")
    private Integer port;

    @ApiModelProperty("Username of the Oracle server")
    private String username;

    @ApiModelProperty("Password of the Oracle server")
    private String password;

    @ApiModelProperty("Database name")
    private String database;

    @ApiModelProperty("Schema name")
    private String schemaName;

    @ApiModelProperty("Table name")
    private String tableName;

    @ApiModelProperty("Scan startup mode")
    private String scanStartupMode;

    @ApiModelProperty("Primary key must be shared by all tables")
    private String primaryKey;

    @ApiModelProperty("Properties for Oracle")
    private Map<String, Object> properties;

    /**
     * Get the dto instance from the request
     */
    public static OracleSourceDTO getFromRequest(OracleSourceRequest request, String extParams) {
        OracleSourceDTO dto = StringUtils.isNotBlank(extParams)
                ? OracleSourceDTO.getFromJson(extParams)
                : new OracleSourceDTO();
        return CommonBeanUtils.copyProperties(request, dto, true);
    }

    public static OracleSourceDTO getFromJson(@NotNull String extParams) {
        try {
            return JsonUtils.parseObject(extParams, OracleSourceDTO.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SOURCE_INFO_INCORRECT,
                    String.format("parse extParams of OracleSource failure: %s", e.getMessage()));
        }
    }

}
