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

package org.apache.inlong.manager.pojo.sink.postgresql;

import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.AESUtils;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.JsonUtils;
import org.apache.inlong.manager.pojo.sink.BaseStreamSink;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * PostgreSQL sink info
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostgreSQLSinkDTO extends BaseStreamSink {

    @ApiModelProperty("JDBC URL of the PostgreSQL server")
    @Length(max = 512, message = "length must be less than or equal to 512")
    @Pattern(regexp = "^((?!\\s).)*$", message = "not supports blank in url")
    private String jdbcUrl;

    @ApiModelProperty("Username for JDBC URL")
    @Length(max = 128, message = "length must be less than or equal to 128")
    private String username;

    @ApiModelProperty("User password")
    @Length(max = 512, message = "length must be less than or equal to 512")
    private String password;

    @ApiModelProperty("Target database name")
    private String dbName;

    @ApiModelProperty("Target table name")
    private String tableName;

    @ApiModelProperty("Primary key")
    private String primaryKey;

    @ApiModelProperty("Password encrypt version")
    private Integer encryptVersion;

    @ApiModelProperty("Properties for PostgreSQL")
    private Map<String, Object> properties;

    /**
     * Get the dto instance from the request
     */
    public static PostgreSQLSinkDTO getFromRequest(PostgreSQLSinkRequest request, String extParams) throws Exception {
        Integer encryptVersion = AESUtils.getCurrentVersion(null);

        PostgreSQLSinkDTO dto = StringUtils.isNotBlank(extParams)
                ? PostgreSQLSinkDTO.getFromJson(extParams)
                : new PostgreSQLSinkDTO();
        CommonBeanUtils.copyProperties(request, dto, true);
        String passwd = dto.getPassword();
        if (StringUtils.isNotEmpty(passwd)) {
            passwd = AESUtils.encryptToString(passwd.getBytes(StandardCharsets.UTF_8), encryptVersion);
        }
        dto.setPassword(passwd);
        dto.setEncryptVersion(encryptVersion);
        return dto;
    }

    /**
     * Get PostgreSQL sink info from JSON string
     *
     * @param extParams JSON string
     * @return PostgreSQL sink DTO
     */
    public static PostgreSQLSinkDTO getFromJson(@NotNull String extParams) {
        try {
            return JsonUtils.parseObject(extParams, PostgreSQLSinkDTO.class).decryptPassword();
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SINK_INFO_INCORRECT,
                    String.format("parse extParams of PostgreSQL SinkDTO failure: %s", e.getMessage()));
        }
    }

    /**
     * Get PostgreSQL table info
     */
    public static PostgreSQLTableInfo getTableInfo(PostgreSQLSinkDTO pgSink, List<PostgreSQLColumnInfo> columnList) {
        PostgreSQLTableInfo tableInfo = new PostgreSQLTableInfo();
        tableInfo.setDbName(pgSink.getDbName());
        tableInfo.setTableName(pgSink.getTableName());
        tableInfo.setPrimaryKey(pgSink.getPrimaryKey());
        tableInfo.setColumns(columnList);
        return tableInfo;
    }

    private PostgreSQLSinkDTO decryptPassword() throws Exception {
        if (StringUtils.isNotEmpty(this.password)) {
            byte[] passwordBytes = AESUtils.decryptAsString(this.password, this.encryptVersion);
            this.password = new String(passwordBytes, StandardCharsets.UTF_8);
        }
        return this;
    }

}
