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

import org.apache.inlong.sort.cdc.base.debezium.history.FlinkJsonTableChangeSerializer;
import org.apache.inlong.sort.cdc.mysql.source.offset.BinlogOffset;
import org.apache.inlong.sort.cdc.mysql.source.split.MySqlMetricSplit.MySqlTableMetric;

import io.debezium.document.Document;
import io.debezium.document.DocumentReader;
import io.debezium.document.DocumentWriter;
import io.debezium.relational.TableId;
import io.debezium.relational.history.TableChanges.TableChange;
import org.apache.flink.core.io.SimpleVersionedSerializer;
import org.apache.flink.core.memory.DataInputDeserializer;
import org.apache.flink.core.memory.DataOutputSerializer;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.table.types.logical.utils.LogicalTypeParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.inlong.sort.cdc.mysql.source.utils.SerializerUtils.readBinlogPosition;
import static org.apache.inlong.sort.cdc.mysql.source.utils.SerializerUtils.rowToSerializedString;
import static org.apache.inlong.sort.cdc.mysql.source.utils.SerializerUtils.serializedStringToRow;
import static org.apache.inlong.sort.cdc.mysql.source.utils.SerializerUtils.writeBinlogPosition;

/**
 * A serializer for the {@link MySqlSplit}.
 */
public final class MySqlSplitSerializer implements SimpleVersionedSerializer<MySqlSplit> {

    public static final MySqlSplitSerializer INSTANCE = new MySqlSplitSerializer();

    private static final int VERSION = 4;
    private static final ThreadLocal<DataOutputSerializer> SERIALIZER_CACHE =
            ThreadLocal.withInitial(() -> new DataOutputSerializer(64));

    private static final int SNAPSHOT_SPLIT_FLAG = 1;
    private static final int BINLOG_SPLIT_FLAG = 2;
    private static final int METRIC_SPLIT_FLAG = 3;

    public static void writeTableSchemas(
            Map<TableId, TableChange> tableSchemas, DataOutputSerializer out) throws IOException {
        FlinkJsonTableChangeSerializer jsonSerializer = new FlinkJsonTableChangeSerializer();
        DocumentWriter documentWriter = DocumentWriter.defaultWriter();
        final int size = tableSchemas.size();
        out.writeInt(size);
        for (Map.Entry<TableId, TableChange> entry : tableSchemas.entrySet()) {
            out.writeUTF(entry.getKey().toString());
            final String tableChangeStr =
                    documentWriter.write(jsonSerializer.toDocument(entry.getValue()));
            final byte[] tableChangeBytes = tableChangeStr.getBytes(StandardCharsets.UTF_8);
            out.writeInt(tableChangeBytes.length);
            out.write(tableChangeBytes);
        }
    }

    public static Map<TableId, TableChange> readTableSchemas(int version, DataInputDeserializer in)
            throws IOException {
        DocumentReader documentReader = DocumentReader.defaultReader();
        Map<TableId, TableChange> tableSchemas = new HashMap<>();
        final int size = in.readInt();
        for (int i = 0; i < size; i++) {
            TableId tableId = TableId.parse(in.readUTF());
            final String tableChangeStr;
            switch (version) {
                case 1:
                    tableChangeStr = in.readUTF();
                    break;
                case 2:
                case 3:
                case 4:
                    final int len = in.readInt();
                    final byte[] bytes = new byte[len];
                    in.read(bytes);
                    tableChangeStr = new String(bytes, StandardCharsets.UTF_8);
                    break;
                default:
                    throw new IOException("Unknown version: " + version);
            }
            Document document = documentReader.read(tableChangeStr);
            TableChange tableChange = FlinkJsonTableChangeSerializer.fromDocument(document, true);
            tableSchemas.put(tableId, tableChange);
        }
        return tableSchemas;
    }

    public static void writeTableDdls(
            Map<TableId, String> tableDdls, DataOutputSerializer out) throws IOException {
        final int size = tableDdls.size();
        out.writeInt(size);
        for (Map.Entry<TableId, String> entry : tableDdls.entrySet()) {
            out.writeUTF(entry.getKey().toString());
            out.writeUTF(entry.getValue());
        }
    }

    public static Map<TableId, String> readTableDdls(int version, DataInputDeserializer in)
            throws IOException {
        Map<TableId, String> tableDdls = new HashMap<>();
        if (in.available() <= 0) {
            return tableDdls;
        }
        final int size = in.readInt();
        for (int i = 0; i < size; i++) {
            TableId tableId = TableId.parse(in.readUTF());
            final String ddl;
            switch (version) {
                case 1:
                case 2:
                case 3:
                case 4:
                    ddl = in.readUTF();
                    break;
                default:
                    throw new IOException("Unknown version: " + version);
            }
            tableDdls.put(tableId, ddl);
        }
        return tableDdls;
    }

    private static void writeFinishedSplitsInfo(
            List<FinishedSnapshotSplitInfo> finishedSplitsInfo, DataOutputSerializer out)
            throws IOException {
        final int size = finishedSplitsInfo.size();
        out.writeInt(size);
        for (FinishedSnapshotSplitInfo splitInfo : finishedSplitsInfo) {
            out.writeUTF(splitInfo.getTableId().toString());
            out.writeUTF(splitInfo.getSplitId());
            out.writeUTF(rowToSerializedString(splitInfo.getSplitStart()));
            out.writeUTF(rowToSerializedString(splitInfo.getSplitEnd()));
            writeBinlogPosition(splitInfo.getHighWatermark(), out);
        }
    }

    private static List<FinishedSnapshotSplitInfo> readFinishedSplitsInfo(
            int version, DataInputDeserializer in) throws IOException {
        List<FinishedSnapshotSplitInfo> finishedSplitsInfo = new ArrayList<>();
        final int size = in.readInt();
        for (int i = 0; i < size; i++) {
            TableId tableId = TableId.parse(in.readUTF());
            String splitId = in.readUTF();
            Object[] splitStart = serializedStringToRow(in.readUTF());
            Object[] splitEnd = serializedStringToRow(in.readUTF());
            BinlogOffset highWatermark = readBinlogPosition(version, in);
            finishedSplitsInfo.add(
                    new FinishedSnapshotSplitInfo(
                            tableId, splitId, splitStart, splitEnd, highWatermark));
        }
        return finishedSplitsInfo;
    }

    private static void writeReadPhaseMetric(Map<String, Long> readPhaseMetrics, DataOutputSerializer out)
            throws IOException {
        final int size = readPhaseMetrics.size();
        out.writeInt(size);
        for (Map.Entry<String, Long> entry : readPhaseMetrics.entrySet()) {
            out.writeUTF(entry.getKey());
            out.writeLong(entry.getValue());
        }
    }

    private static Map<String, Long> readReadPhaseMetric(DataInputDeserializer in) throws IOException {
        Map<String, Long> readPhaseMetrics = new HashMap<>();
        if (in.available() > 0) {
            final int size = in.readInt();
            for (int i = 0; i < size; i++) {
                readPhaseMetrics.put(in.readUTF(), in.readLong());
            }
        }
        return readPhaseMetrics;
    }

    private static void writeTableMetrics(Map<String, MySqlTableMetric> tableMetrics, DataOutputSerializer out)
            throws IOException {
        final int size = tableMetrics.size();
        out.writeInt(size);
        for (Map.Entry<String, MySqlTableMetric> entry : tableMetrics.entrySet()) {
            out.writeUTF(entry.getKey());
            out.writeLong(entry.getValue().getNumRecordsIn());
            out.writeLong(entry.getValue().getNumBytesIn());
        }
    }

    private static Map<String, MySqlTableMetric> readTableMetrics(DataInputDeserializer in) throws IOException {
        Map<String, MySqlTableMetric> tableMetrics = new HashMap<>();
        if (in.available() > 0) {
            final int size = in.readInt();
            for (int i = 0; i < size; i++) {
                String tableIdentify = in.readUTF();
                tableMetrics.put(tableIdentify, new MySqlTableMetric(in.readLong(), in.readLong()));
            }
        }
        return tableMetrics;
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public byte[] serialize(MySqlSplit split) throws IOException {
        if (split.isSnapshotSplit()) {
            final MySqlSnapshotSplit snapshotSplit = split.asSnapshotSplit();
            // optimization: the splits lazily cache their own serialized form
            if (snapshotSplit.serializedFormCache != null) {
                return snapshotSplit.serializedFormCache;
            }

            final DataOutputSerializer out = SERIALIZER_CACHE.get();
            out.writeInt(SNAPSHOT_SPLIT_FLAG);
            out.writeUTF(snapshotSplit.getTableId().toString());
            out.writeUTF(snapshotSplit.splitId());
            out.writeUTF(snapshotSplit.getSplitKeyType().asSerializableString());

            final Object[] splitStart = snapshotSplit.getSplitStart();
            final Object[] splitEnd = snapshotSplit.getSplitEnd();
            // rowToSerializedString deals null case
            out.writeUTF(rowToSerializedString(splitStart));
            out.writeUTF(rowToSerializedString(splitEnd));
            writeBinlogPosition(snapshotSplit.getHighWatermark(), out);
            writeTableSchemas(snapshotSplit.getTableSchemas(), out);
            final byte[] result = out.getCopyOfBuffer();
            out.clear();
            // optimization: cache the serialized from, so we avoid the byte work during repeated
            // serialization
            snapshotSplit.serializedFormCache = result;
            return result;
        } else if (split.isBinlogSplit()) {
            final MySqlBinlogSplit binlogSplit = split.asBinlogSplit();
            // optimization: the splits lazily cache their own serialized form
            if (binlogSplit.serializedFormCache != null) {
                return binlogSplit.serializedFormCache;
            }
            final DataOutputSerializer out = SERIALIZER_CACHE.get();
            out.writeInt(BINLOG_SPLIT_FLAG);
            out.writeUTF(binlogSplit.splitId());
            out.writeUTF("");
            writeBinlogPosition(binlogSplit.getStartingOffset(), out);
            writeBinlogPosition(binlogSplit.getEndingOffset(), out);
            writeFinishedSplitsInfo(binlogSplit.getFinishedSnapshotSplitInfos(), out);
            writeTableSchemas(binlogSplit.getTableSchemas(), out);
            out.writeInt(binlogSplit.getTotalFinishedSplitSize());
            out.writeBoolean(binlogSplit.isSuspended());
            writeTableDdls(binlogSplit.getTableDdls(), out);
            final byte[] result = out.getCopyOfBuffer();
            out.clear();
            // optimization: cache the serialized from, so we avoid the byte work during repeated
            // serialization
            binlogSplit.serializedFormCache = result;
            return result;
        } else {
            final MySqlMetricSplit mysqlMetricSplit = split.asMetricSplit();
            final DataOutputSerializer out = SERIALIZER_CACHE.get();
            out.writeInt(METRIC_SPLIT_FLAG);
            out.writeLong(mysqlMetricSplit.getNumBytesIn());
            out.writeLong(mysqlMetricSplit.getNumRecordsIn());
            writeReadPhaseMetric(mysqlMetricSplit.getReadPhaseMetricMap(), out);
            writeTableMetrics(mysqlMetricSplit.getTableMetricMap(), out);
            final byte[] result = out.getCopyOfBuffer();
            out.clear();
            return result;
        }
    }

    @Override
    public MySqlSplit deserialize(int version, byte[] serialized) throws IOException {
        switch (version) {
            case 1:
            case 2:
            case 3:
            case 4:
                return deserializeSplit(version, serialized);
            default:
                throw new IOException("Unknown version: " + version);
        }
    }

    /**
     * deserialize
     */
    public MySqlSplit deserializeSplit(int version, byte[] serialized) throws IOException {
        final DataInputDeserializer in = new DataInputDeserializer(serialized);

        int splitKind = in.readInt();
        if (splitKind == SNAPSHOT_SPLIT_FLAG) {
            TableId tableId = TableId.parse(in.readUTF());
            String splitId = in.readUTF();
            RowType splitKeyType = (RowType) LogicalTypeParser.parse(in.readUTF());
            Object[] splitBoundaryStart = serializedStringToRow(in.readUTF());
            Object[] splitBoundaryEnd = serializedStringToRow(in.readUTF());
            BinlogOffset highWatermark = readBinlogPosition(version, in);
            Map<TableId, TableChange> tableSchemas = readTableSchemas(version, in);

            return new MySqlSnapshotSplit(
                    tableId,
                    splitId,
                    splitKeyType,
                    splitBoundaryStart,
                    splitBoundaryEnd,
                    highWatermark,
                    tableSchemas);
        } else if (splitKind == BINLOG_SPLIT_FLAG) {
            final String splitId = in.readUTF();
            // skip split Key Type
            in.readUTF();
            BinlogOffset startingOffset = readBinlogPosition(version, in);
            BinlogOffset endingOffset = readBinlogPosition(version, in);
            List<FinishedSnapshotSplitInfo> finishedSplitsInfo =
                    readFinishedSplitsInfo(version, in);
            Map<TableId, TableChange> tableChangeMap = readTableSchemas(version, in);
            int totalFinishedSplitSize = finishedSplitsInfo.size();
            boolean isSuspended = false;
            Map<TableId, String> tableDdls = null;
            if (version >= 3) {
                totalFinishedSplitSize = in.readInt();
                if (version > 3) {
                    isSuspended = in.readBoolean();
                    tableDdls = readTableDdls(version, in);
                }
            }
            in.releaseArrays();
            return new MySqlBinlogSplit(
                    splitId,
                    startingOffset,
                    endingOffset,
                    finishedSplitsInfo,
                    tableChangeMap,
                    totalFinishedSplitSize,
                    isSuspended,
                    tableDdls);
        } else if (splitKind == METRIC_SPLIT_FLAG) {
            long numBytesIn = 0L;
            long numRecordsIn = 0L;
            if (in.available() > 0) {
                numBytesIn = in.readLong();
                numRecordsIn = in.readLong();
            }
            Map<String, Long> readPhaseMetricMap = readReadPhaseMetric(in);
            Map<String, MySqlTableMetric> tableMetricMap = readTableMetrics(in);

            return new MySqlMetricSplit(numBytesIn, numRecordsIn, readPhaseMetricMap, tableMetricMap);
        } else {
            throw new IOException("Unknown split kind: " + splitKind);
        }
    }
}
