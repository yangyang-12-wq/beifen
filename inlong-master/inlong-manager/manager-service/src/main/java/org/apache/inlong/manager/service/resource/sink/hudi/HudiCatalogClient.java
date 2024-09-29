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

package org.apache.inlong.manager.service.resource.sink.hudi;

import org.apache.inlong.manager.pojo.sink.hudi.HudiColumnInfo;
import org.apache.inlong.manager.pojo.sink.hudi.HudiTableInfo;

import com.google.common.collect.Maps;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.HiveMetaStoreClient;
import org.apache.hadoop.hive.metastore.IMetaStoreClient;
import org.apache.hadoop.hive.metastore.TableType;
import org.apache.hadoop.hive.metastore.api.AlreadyExistsException;
import org.apache.hadoop.hive.metastore.api.Database;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.MetaException;
import org.apache.hadoop.hive.metastore.api.Order;
import org.apache.hadoop.hive.metastore.api.SerDeInfo;
import org.apache.hadoop.hive.metastore.api.SkewedInfo;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.hadoop.hive.metastore.api.hive_metastoreConstants;
import org.apache.hadoop.hive.serde.serdeConstants;
import org.apache.hadoop.hive.serde2.MetadataTypedColumnsetSerDe;
import org.apache.hadoop.mapred.SequenceFileInputFormat;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.inlong.manager.service.resource.sink.hudi.HudiUtils.IS_QUERY_AS_RO_TABLE;
import static org.apache.inlong.manager.service.resource.sink.hudi.HudiUtils.getInputFormatClassName;
import static org.apache.inlong.manager.service.resource.sink.hudi.HudiUtils.getOutputFormatClassName;
import static org.apache.inlong.manager.service.resource.sink.hudi.HudiUtils.getSerDeClassName;

/**
 * The Catalog client for Hudi.
 */
public class HudiCatalogClient implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(HudiCatalogClient.class);

    private String dbName;
    private final String warehouse;
    private IMetaStoreClient client;
    private final HiveConf hiveConf;

    public HudiCatalogClient(String uri, String warehouse, String dbName) throws MetaException {
        this.warehouse = warehouse;
        this.dbName = dbName;
        hiveConf = new HiveConf();
        hiveConf.setVar(HiveConf.ConfVars.METASTOREURIS, uri);
        hiveConf.setBoolVar(HiveConf.ConfVars.METASTORE_EXECUTE_SET_UGI, false);
    }

    public HudiCatalogClient(String uri, String warehouse) throws MetaException {
        this.warehouse = warehouse;
        hiveConf = new HiveConf();
        hiveConf.setVar(HiveConf.ConfVars.METASTOREURIS, uri);
        hiveConf.setBoolVar(HiveConf.ConfVars.METASTORE_EXECUTE_SET_UGI, false);
    }

    /**
     * Open the hive metastore connection
     */
    public void open() {
        if (this.client == null) {
            try {
                this.client = new HiveMetaStoreClient(hiveConf);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create hive metastore client", e);
            }
            LOG.info("Connected to Hive metastore");
        }
    }

    private void createDatabase(String warehouse, Map<String, String> meta, boolean ignoreIfExists) {
        Database database = new Database();
        Map<String, String> parameter = Maps.newHashMap();
        database.setName(dbName);
        database.setLocationUri((new Path(warehouse, dbName) + ".db"));
        meta.forEach((key, value) -> {
            if (key.equals("comment")) {
                database.setDescription(value);
            } else if (key.equals("location")) {
                database.setLocationUri(value);
            } else if (value != null) {
                parameter.put(key, value);
            }
            database.setParameters(parameter);
        });
        try {
            client.createDatabase(database);
        } catch (AlreadyExistsException e) {
            if (!ignoreIfExists) {
                throw new RuntimeException("Database '" + dbName + "' already exist!");
            }
        } catch (TException e) {
            throw new RuntimeException("Failed to create database '" + dbName
                    + "'", e);
        }
    }

    /**
     * Create the hudi database
     * @param warehouse the warehouse directory in dfs
     * @param ignoreIfExists not create again if exist 
     */
    public void createDatabase(String warehouse, boolean ignoreIfExists) {
        createDatabase(warehouse, Maps.newHashMap(), ignoreIfExists);
    }

    /**
     * Check table if exist
     * @param tableName the table name of hudi table
     * @return return true if exist
     */
    public boolean tableExist(String tableName) throws TException {
        return client.tableExists(dbName, tableName);
    }

    /**
     * get column infos of exist hudi table
     * @param dbName the database name
     * @param tableName the table name
     */
    public List<HudiColumnInfo> getColumns(
            String dbName,
            String tableName)
            throws TException {
        Table hiveTable = client.getTable(dbName, tableName);
        List<FieldSchema> allCols = hiveTable.getSd().getCols().stream()
                // filter out the metadata columns
                .filter(s -> !HudiUtils.isMetadataFile(s.getName()))
                .collect(Collectors.toList());

        return allCols.stream()
                .map((FieldSchema s) -> {
                    HudiColumnInfo info = new HudiColumnInfo();
                    info.setName(s.getName());
                    info.setType(s.getType());
                    return info;
                })
                .collect(Collectors.toList());
    }

    /**
     * Add column to hudi table at the tail
     */
    public void addColumns(String tableName, List<HudiColumnInfo> columns) throws TException {
        Table hiveTable = client.getTable(dbName, tableName);
        Table newHiveTable = hiveTable.deepCopy();
        List<FieldSchema> cols = newHiveTable.getSd().getCols();
        for (HudiColumnInfo column : columns) {
            FieldSchema fieldSchema = new FieldSchema();
            fieldSchema.setName(column.getName());
            fieldSchema.setType(column.getType());
            fieldSchema.setComment(column.getDesc());
            cols.add(fieldSchema);
        }
        newHiveTable.getSd().setCols(cols);
        client.alter_table(dbName, tableName, newHiveTable);
    }

    /**
     * Create hudi table and register to hive metastore
     * @param tableName the hudi table name
     * @param tableInfo the hudi table info
     * @param useRealTimeInputFormat ignore uber input Format 
     */
    public void createTable(
            String tableName,
            HudiTableInfo tableInfo,
            boolean useRealTimeInputFormat)
            throws TException, IOException {
        Table hiveTable = this.getEmptyTable(dbName, tableName);
        hiveTable.setOwner(UserGroupInformation.getCurrentUser().getUserName());
        hiveTable.setCreateTime((int) (System.currentTimeMillis() / 1000));

        Map<String, String> properties = new HashMap<>();
        String location = this.warehouse + "/" + dbName + ".db" + "/" + tableName;
        properties.put("path", location);

        List<FieldSchema> cols = tableInfo.getColumns()
                .stream()
                .map(column -> {
                    FieldSchema fieldSchema = new FieldSchema();
                    fieldSchema.setName(column.getName());
                    fieldSchema.setType(HudiTypeConverter.convert(column));
                    fieldSchema.setComment(column.getDesc());
                    return fieldSchema;
                })
                .collect(Collectors.toList());

        // Build storage of hudi table
        StorageDescriptor sd = new StorageDescriptor();
        sd.setCols(cols);
        hiveTable.setDbName(dbName);
        hiveTable.setTableName(tableName);
        // FIXME: splitSchemas need config by frontend

        HudiFileFormat baseFileFormat = HudiFileFormat.PARQUET;
        // ignore uber input Format
        String inputFormatClassName =
                getInputFormatClassName(baseFileFormat, useRealTimeInputFormat);
        String outputFormatClassName = getOutputFormatClassName(baseFileFormat);
        String serDeClassName = getSerDeClassName(baseFileFormat);
        sd.setInputFormat(inputFormatClassName);
        sd.setOutputFormat(outputFormatClassName);

        Map<String, String> serdeProperties = new HashMap<>();
        serdeProperties.put("path", location);
        serdeProperties.put(IS_QUERY_AS_RO_TABLE, String.valueOf(!useRealTimeInputFormat));
        sd.setSerdeInfo(new SerDeInfo(null, serDeClassName, serdeProperties));
        sd.setLocation(location);
        hiveTable.setSd(sd);

        hiveTable.setParameters(properties);

        client.createTable(hiveTable);
    }

    public List<String> listAllDatabases() throws TException {
        return client.getAllDatabases();
    }

    /**
     * Close the connection of hive metastore
     */
    public void close() {
        if (client != null) {
            client.close();
            client = null;
            LOG.info("Disconnect to hive metastore");
        }
    }

    public Table getEmptyTable(String databaseName, String tableName) {
        StorageDescriptor sd = new StorageDescriptor();
        {
            sd.setSerdeInfo(new SerDeInfo());
            sd.setNumBuckets(-1);
            sd.setBucketCols(new ArrayList<String>());
            sd.setCols(new ArrayList<FieldSchema>());
            sd.setParameters(new HashMap<String, String>());
            sd.setSortCols(new ArrayList<Order>());
            sd.getSerdeInfo().setParameters(new HashMap<String, String>());
            // We have to use MetadataTypedColumnsetSerDe because LazySimpleSerDe does
            // not support a table with no columns.
            sd.getSerdeInfo().setSerializationLib(MetadataTypedColumnsetSerDe.class.getName());
            sd.getSerdeInfo().getParameters().put(serdeConstants.SERIALIZATION_FORMAT, "1");
            sd.setInputFormat(SequenceFileInputFormat.class.getName());
            SkewedInfo skewInfo = new SkewedInfo();
            skewInfo.setSkewedColNames(new ArrayList<String>());
            skewInfo.setSkewedColValues(new ArrayList<List<String>>());
            skewInfo.setSkewedColValueLocationMaps(new HashMap<List<String>, String>());
            sd.setSkewedInfo(skewInfo);
        }

        org.apache.hadoop.hive.metastore.api.Table t = new org.apache.hadoop.hive.metastore.api.Table();
        {
            t.setSd(sd);
            t.setPartitionKeys(new ArrayList<FieldSchema>());
            t.setParameters(new HashMap<String, String>());
            t.setTableType(TableType.MANAGED_TABLE.toString());
            t.setDbName(databaseName);
            t.setTableName(tableName);
            // set create time
            t.setCreateTime((int) (System.currentTimeMillis() / 1000));
        }
        // Explictly set the bucketing version
        t.getParameters().put(hive_metastoreConstants.TABLE_BUCKETING_VERSION,
                "2");
        return t;
    }

}
