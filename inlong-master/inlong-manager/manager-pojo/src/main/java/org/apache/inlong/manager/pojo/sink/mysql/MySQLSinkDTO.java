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

package org.apache.inlong.manager.pojo.sink.mysql;

import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.JsonUtils;
import org.apache.inlong.manager.pojo.sink.BaseStreamSink;
import org.apache.inlong.manager.pojo.util.MySQLSensitiveUrlUtils;

import com.google.common.base.Strings;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MySQL sink info
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MySQLSinkDTO extends BaseStreamSink {

    private static final Logger LOGGER = LoggerFactory.getLogger(MySQLSinkDTO.class);
    private static final String MYSQL_JDBC_PREFIX = "jdbc:mysql://";

    @ApiModelProperty("MySQL JDBC URL, such as jdbc:mysql://host:port")
    private String jdbcUrl;

    @ApiModelProperty("Username for JDBC URL")
    private String username;

    @ApiModelProperty("User password")
    private String password;

    @ApiModelProperty("Target database name")
    private String databaseName;

    @ApiModelProperty("Target table name")
    private String tableName;

    @ApiModelProperty("Primary key")
    private String primaryKey;

    @ApiModelProperty("Properties for MySQL")
    private Map<String, Object> properties;

    /**
     * Get the dto instance from the request
     *
     * @param request MySQLSinkRequest
     * @return {@link MySQLSinkDTO}
     * @apiNote The config here will be saved to the database, so filter sensitive params before saving.
     */
    public static MySQLSinkDTO getFromRequest(MySQLSinkRequest request, String extParams) {
        MySQLSinkDTO dto = StringUtils.isNotBlank(extParams) ? MySQLSinkDTO.getFromJson(extParams) : new MySQLSinkDTO();
        CommonBeanUtils.copyProperties(request, dto, true);
        dto.setJdbcUrl(filterSensitive(request.getJdbcUrl()));
        return dto;
    }

    /**
     * Get MySQL sink info from JSON string
     *
     * @param extParams string ext params
     * @return {@link MySQLSinkDTO}
     */
    public static MySQLSinkDTO getFromJson(@NotNull String extParams) {
        try {
            return JsonUtils.parseObject(extParams, MySQLSinkDTO.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SINK_INFO_INCORRECT,
                    String.format("parse extParams of MySQL SinkDTO failure: %s", e.getMessage()));
        }
    }

    /**
     * Get MySQL table info
     *
     * @param mySQLSink MySQL sink dto,{@link MySQLSinkDTO}
     * @param columnList MySQL column info list,{@link MySQLColumnInfo}
     * @return {@link MySQLTableInfo}
     */
    public static MySQLTableInfo getTableInfo(MySQLSinkDTO mySQLSink, List<MySQLColumnInfo> columnList) {
        MySQLTableInfo tableInfo = new MySQLTableInfo();
        tableInfo.setDbName(mySQLSink.getDatabaseName());
        tableInfo.setTableName(mySQLSink.getTableName());
        tableInfo.setPrimaryKey(mySQLSink.getPrimaryKey());
        tableInfo.setColumns(columnList);
        return tableInfo;
    }

    /**
     * Get DbName from jdbcUrl
     *
     * @param jdbcUrl MySQL JDBC url, such as jdbc:mysql://host:port/database
     * @return database name
     */
    private static String getDbNameFromUrl(String jdbcUrl) {
        String database = null;

        if (Strings.isNullOrEmpty(jdbcUrl)) {
            throw new IllegalArgumentException("Invalid JDBC url.");
        }

        jdbcUrl = jdbcUrl.toLowerCase();
        if (jdbcUrl.startsWith("jdbc:impala")) {
            jdbcUrl = jdbcUrl.replace(":impala", "");
        }

        int pos1;
        if (!jdbcUrl.startsWith("jdbc:")
                || (pos1 = jdbcUrl.indexOf(':', 5)) == -1) {
            throw new IllegalArgumentException("Invalid JDBC url.");
        }

        String connUri = jdbcUrl.substring(pos1 + 1);
        if (connUri.startsWith("//")) {
            int pos = connUri.indexOf('/', 2);
            if (pos != -1) {
                database = connUri.substring(pos + 1);
            }
        } else {
            database = connUri;
        }

        if (Strings.isNullOrEmpty(database)) {
            throw new IllegalArgumentException("Invalid JDBC URL: " + jdbcUrl);
        }

        if (database.contains(InlongConstants.QUESTION_MARK)) {
            database = database.substring(0, database.indexOf(InlongConstants.QUESTION_MARK));
        }
        if (database.contains(InlongConstants.SEMICOLON)) {
            database = database.substring(0, database.indexOf(InlongConstants.SEMICOLON));
        }
        return database;
    }

    public static String setDbNameToUrl(String jdbcUrl, String databaseName) {
        if (StringUtils.isBlank(jdbcUrl)) {
            return jdbcUrl;
        }
        String pattern = "jdbc:mysql://(?<host>[a-zA-Z0-9-//.]+):(?<port>[0-9]+)?(?<ext>)";
        Pattern namePattern = Pattern.compile(pattern);
        Matcher dataMatcher = namePattern.matcher(jdbcUrl);
        StringBuilder resultUrl;
        if (dataMatcher.find()) {
            String host = dataMatcher.group("host");
            String port = dataMatcher.group("port");
            resultUrl = new StringBuilder().append(MYSQL_JDBC_PREFIX)
                    .append(host)
                    .append(InlongConstants.COLON)
                    .append(port)
                    .append(InlongConstants.SLASH)
                    .append(databaseName);
        } else {
            throw new BusinessException(ErrorCodeEnum.SINK_INFO_INCORRECT,
                    "MySQL JDBC URL was invalid, it should like jdbc:mysql://host:port");
        }
        if (jdbcUrl.contains(InlongConstants.QUESTION_MARK)) {
            resultUrl.append(jdbcUrl.substring(jdbcUrl.indexOf(InlongConstants.QUESTION_MARK)));
        }
        return resultUrl.toString();
    }

    public static String filterSensitive(String url) {
        return MySQLSensitiveUrlUtils.filterSensitive(url);
    }

}
