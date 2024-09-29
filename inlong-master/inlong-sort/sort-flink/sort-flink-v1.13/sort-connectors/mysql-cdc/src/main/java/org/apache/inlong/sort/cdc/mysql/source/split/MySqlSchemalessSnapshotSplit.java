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

package org.apache.inlong.sort.cdc.mysql.source.split;

import org.apache.inlong.sort.cdc.mysql.source.offset.BinlogOffset;

import io.debezium.relational.TableId;
import io.debezium.relational.history.TableChanges.TableChange;
import org.apache.flink.table.types.logical.RowType;

import java.util.HashMap;
import java.util.Map;

/**
 * The MySqlSnapshotSplit without schema information to reduce the memory usage.
 * ref {@link com.ververica.cdc.connectors.mysql.source.split.MySqlSchemalessSnapshotSplit}
 */
public class MySqlSchemalessSnapshotSplit extends MySqlSnapshotSplit {

    public MySqlSchemalessSnapshotSplit(
            TableId tableId,
            String splitId,
            RowType splitKeyType,
            Object[] splitStart,
            Object[] splitEnd,
            BinlogOffset highWatermark) {
        super(
                tableId,
                splitId,
                splitKeyType,
                splitStart,
                splitEnd,
                highWatermark,
                new HashMap<>(1));
    }

    /**
     * Converts current {@link MySqlSchemalessSnapshotSplit} to {@link com.ververica.cdc.connectors.mysql.source.split.MySqlSnapshotSplit} with
     * given table schema information.
     */
    public final MySqlSnapshotSplit toMySqlSnapshotSplit(
            TableChange tableSchema) {
        Map<TableId, TableChange> tableSchemas = new HashMap<>();
        tableSchemas.put(getTableId(), tableSchema);
        return new MySqlSnapshotSplit(
                getTableId(),
                splitId(),
                getSplitKeyType(),
                getSplitStart(),
                getSplitEnd(),
                getHighWatermark(),
                tableSchemas);
    }
}
