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

package org.apache.inlong.audit.config;

/**
 * Sql constants
 */
public class SqlConstants {

    // HA selector sql
    public static final String SELECTOR_SQL =
            "insert ignore into {0} (service_id, leader_id, last_seen_active) values (''{1}'', ''{2}'', now()) on duplicate key update leader_id = if(last_seen_active < now() - interval # second, values(leader_id), leader_id),last_seen_active = if(leader_id = values(leader_id), values(last_seen_active), last_seen_active)";
    public static final String REPLACE_LEADER_SQL =
            "replace into {0} ( service_id, leader_id, last_seen_active ) values (''{1}'', ''#'', now())";
    public static final String RELEASE_SQL = "delete from {0} where service_id=''{1}'' and leader_id= ''{2}''";
    public static final String IS_LEADER_SQL =
            "select count(*) as is_leader from {0} where service_id=''{1}'' and leader_id=''{2}''";
    public static final String SEARCH_CURRENT_LEADER_SQL =
            "select leader_id as leader from {0} where service_id=''{1}''";
    public static final String SELECT_TEST_SQL = "SELECT 1 ";

    // Source query sql
    public static final String KEY_SOURCE_STAT_SQL = "source.stat.sql";
    public static final String DEFAULT_SOURCE_STAT_SQL =
            "SELECT inlong_group_id, inlong_stream_id, audit_id, audit_tag\n" +
                    "\t, SUM(cnt) AS cnt, SUM(size) AS size\n" +
                    "\t, SUM(delay) AS delay\n" +
                    "FROM (\n" +
                    "\tSELECT t_all_version.log_ts, t_all_version.inlong_group_id, t_all_version.inlong_stream_id, t_all_version.audit_id, t_all_version.audit_tag\n"
                    +
                    "\t\t, t_all_version.cnt, t_all_version.size, t_all_version.delay\n" +
                    "\tFROM (\n" +
                    "\t\tSELECT audit_version, log_ts, inlong_group_id, inlong_stream_id, audit_id\n" +
                    "\t\t\t, " +
                    "CASE \n" +
                    "    WHEN audit_tag = '' THEN '-1'\n" +
                    "    ELSE audit_tag\n" +
                    "END AS audit_tag " +
                    ", SUM(count) AS cnt, SUM(size) AS size\n" +
                    "\t\t\t, SUM(delay) AS delay\n" +
                    "\t\tFROM audit_data\n" +
                    "\t\tWHERE log_ts >= ? AND log_ts < ? \n" +
                    "\t\t\tAND audit_id = ?\n" +
                    "\t\tGROUP BY audit_version, log_ts, inlong_group_id, inlong_stream_id, audit_id, audit_tag\n" +
                    "\t) t_all_version\n" +
                    "\t\tJOIN (\n" +
                    "\t\t\tSELECT max(audit_version) AS audit_version, log_ts, inlong_group_id, inlong_stream_id\n" +
                    "\t\t\t\t, audit_id, " +
                    "CASE \n" +
                    "    WHEN audit_tag = '' THEN '-1'\n" +
                    "    ELSE audit_tag\n" +
                    "END AS audit_tag \n" +
                    "\t\t\tFROM audit_data\n" +
                    "\t\t\tWHERE log_ts >= ? AND log_ts < ? \n" +
                    "\t\t\t\tAND audit_id = ?\n" +
                    "\t\t\tGROUP BY log_ts, inlong_group_id, inlong_stream_id, audit_id, audit_tag\n" +
                    "\t\t) t_max_version\n" +
                    "\t\tON t_all_version.audit_version = t_max_version.audit_version\n" +
                    "\t\t\tAND t_all_version.log_ts = t_max_version.log_ts\n" +
                    "\t\t\tAND t_all_version.inlong_group_id = t_max_version.inlong_group_id\n" +
                    "\t\t\tAND t_all_version.inlong_stream_id = t_max_version.inlong_stream_id\n" +
                    "\t\t\tAND t_all_version.audit_id = t_max_version.audit_id\n" +
                    "\t\t\tAND t_all_version.audit_tag = t_max_version.audit_tag\n" +
                    ") t_sum\n" +
                    "GROUP BY inlong_group_id, inlong_stream_id, audit_id, audit_tag";

    public static final String KEY_SOURCE_QUERY_IPS_SQL = "source.query.ips.sql";
    public static final String DEFAULT_SOURCE_QUERY_IPS_SQL =
            "SELECT ip, sum(count) AS cnt, sum(size) AS size\n" +
                    "\t, sum(delay) AS delay\n" +
                    "FROM audit_data\n" +
                    "WHERE log_ts >= ? AND log_ts < ? \n" +
                    "\tAND inlong_group_id = ? \n" +
                    "\tAND inlong_stream_id =  ? \n" +
                    "\tAND audit_id =  ? \n" +
                    "GROUP BY ip ";

    public static final String KEY_SOURCE_QUERY_IDS_SQL = "source.query.ids.sql";
    public static final String DEFAULT_SOURCE_QUERY_IDS_SQL =
            "SELECT inlong_group_id, inlong_stream_id, audit_id, " +
                    "CASE \n" +
                    "    WHEN audit_tag = '' THEN '-1'\n" +
                    "    ELSE audit_tag\n" +
                    "END AS audit_tag \n" +
                    "\t, sum(count) AS cnt, sum(size) AS size\n" +
                    "\t, sum(delay) AS delay\n" +
                    "FROM audit_data\n" +
                    "WHERE log_ts >= ? AND log_ts < ? \n" +
                    "\tAND audit_id = ? \n" +
                    "\tAND ip = ? \n" +
                    "GROUP BY inlong_group_id, inlong_stream_id, audit_id, audit_tag";

    public static final String KEY_SOURCE_QUERY_MINUTE_SQL = "source.query.minute.sql";
    public static final String DEFAULT_SOURCE_QUERY_MINUTE_SQL =
            "SELECT log_ts, inlong_group_id, inlong_stream_id, audit_id, audit_tag\n" +
                    "\t, sum(count) AS cnt, sum(size) AS size\n" +
                    "\t, sum(delay) AS delay, audit_version\n" +
                    "FROM (\n" +
                    "\tSELECT audit_version, docker_id, thread_id, sdk_ts, packet_id\n" +
                    "\t\t, log_ts, ip, inlong_group_id, inlong_stream_id, audit_id\n" +
                    "\t\t, " +
                    "   CASE \n" +
                    "        WHEN audit_tag ='' THEN '-1'\n" +
                    "        ELSE audit_tag\n" +
                    "    END AS audit_tag ," +
                    " count, size, delay\n" +
                    "\tFROM audit_data\n" +
                    "\tWHERE log_ts >= ? AND log_ts < ? \n" +
                    "\t\tAND inlong_group_id = ?\n" +
                    "\t\tAND inlong_stream_id = ?\n" +
                    "\t\tAND audit_id = ?\n" +
                    "\tGROUP BY audit_version, docker_id, thread_id, sdk_ts, packet_id, log_ts, ip, inlong_group_id, inlong_stream_id, audit_id, audit_tag, count, size, delay\n"
                    +
                    ") t_distinct\n" +
                    "GROUP BY audit_version, log_ts, inlong_group_id, inlong_stream_id, audit_id, audit_tag\n" +
                    "LIMIT 1440";

    // Mysql query sql
    public static final String KEY_MYSQL_SOURCE_QUERY_TEMP_SQL = "mysql.query.temp.sql";
    public static final String DEFAULT_MYSQL_SOURCE_QUERY_TEMP_SQL =
            "SELECT inlong_group_id, inlong_stream_id, audit_id, audit_tag\n" +
                    ", sum(count) AS cnt, sum(size) AS size\n" +
                    ", sum(delay) AS delay\n" +
                    "FROM audit_data_temp\n" +
                    "WHERE log_ts >= ? AND log_ts < ? \n" +
                    "AND audit_id = ? \n" +
                    "GROUP BY inlong_group_id, inlong_stream_id, audit_id, audit_tag";

    public static final String KEY_MYSQL_SOURCE_QUERY_DAY_SQL = "mysql.query.day.sql";
    public static final String DEFAULT_MYSQL_SOURCE_QUERY_DAY_SQL =
            "select log_ts,inlong_group_id,inlong_stream_id,audit_id,audit_tag,count,size,delay " +
                    "from audit_data_day where log_ts >= ? AND log_ts < ? AND inlong_group_id=? AND inlong_stream_id=? AND audit_id =? ";

    public static final String KEY_MYSQL_QUERY_AUDIT_ID_SQL = "mysql.query.audit.id.sql";
    public static final String DEFAULT_MYSQL_QUERY_AUDIT_ID_SQL =
            "select audit_id from audit_id_config where status=1 ";

    public static final String KEY_MYSQL_QUERY_AUDIT_SOURCE_SQL = "mysql.query.audit.source.sql";
    public static final String DEFAULT_MYSQL_QUERY_AUDIT_SOURCE_SQL =
            "select jdbc_driver_class, jdbc_url, jdbc_user_name, jdbc_password, service_id from audit_source_config where status=1 ";

    // Mysql insert sql
    public static final String KEY_MYSQL_SINK_INSERT_DAY_SQL = "mysql.sink.insert.day.sql";
    public static final String DEFAULT_MYSQL_SINK_INSERT_DAY_SQL =
            "replace into audit_data_day (log_ts,inlong_group_id, inlong_stream_id, audit_id,audit_tag,count, size, delay) "
                    + " values (?,?,?,?,?,?,?,?)";
    public static final String KEY_MYSQL_SINK_INSERT_TEMP_SQL = "mysql.sink.insert.temp.sql";
    public static final String DEFAULT_MYSQL_SINK_INSERT_TEMP_SQL =
            "replace into audit_data_temp (log_ts,inlong_group_id, inlong_stream_id, audit_id,audit_tag,count, size, delay) "
                    + " values (?,?,?,?,?,?,?,?)";
    public static final String KEY_AUDIT_DATA_TEMP_ADD_PARTITION_SQL = "audit.data.temp.add.partition.sql";
    public static final String DEFAULT_AUDIT_DATA_TEMP_ADD_PARTITION_SQL =
            "ALTER TABLE audit_data_temp ADD PARTITION (PARTITION %s VALUES LESS THAN (TO_DAYS('%s')))";

    public static final String KEY_AUDIT_DATA_TEMP_DELETE_PARTITION_SQL = "audit.data.temp.delete.partition.sql";
    public static final String DEFAULT_AUDIT_DATA_TEMP_DELETE_PARTITION_SQL =
            "ALTER TABLE audit_data_temp DROP PARTITION %s";

    public static final String KEY_AUDIT_DATA_TEMP_CHECK_PARTITION_SQL = "audit.data.temp.check.partition.sql";
    public static final String DEFAULT_AUDIT_DATA_TEMP_CHECK_PARTITION_SQL =
            "SELECT COUNT(*) AS count FROM INFORMATION_SCHEMA.PARTITIONS WHERE TABLE_NAME = 'audit_data_temp' and PARTITION_NAME = ?";
}
