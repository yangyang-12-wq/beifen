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

package org.apache.inlong.sort.cdc.mongodb.source.assigners.splitters;

import org.apache.inlong.sort.cdc.mongodb.source.config.MongoDBSourceConfig;

import com.mongodb.client.MongoClient;
import io.debezium.relational.TableId;
import org.apache.flink.annotation.Internal;
import org.bson.BsonBoolean;
import org.bson.BsonDocument;
import org.bson.BsonInt64;

import static org.apache.inlong.sort.cdc.mongodb.source.utils.MongoUtils.clientFor;
import static org.apache.inlong.sort.cdc.mongodb.source.utils.MongoUtils.collStats;

/**
 * The split context used by {@link SplitStrategy} to split collection into a set of chunks for
 * MongoDB data source.
 * Copy from com.ververica:flink-connector-mongodb-cdc:2.3.0.
 */
@Internal
public class SplitContext {

    private final MongoClient mongoClient;
    private final TableId collectionId;
    private final BsonDocument collectionStats;
    private final int chunkSizeMB;

    public SplitContext(
            MongoClient mongoClient,
            TableId collectionId,
            BsonDocument collectionStats,
            int chunkSizeMB) {
        this.mongoClient = mongoClient;
        this.collectionId = collectionId;
        this.collectionStats = collectionStats;
        this.chunkSizeMB = chunkSizeMB;
    }

    public static SplitContext of(MongoDBSourceConfig sourceConfig, TableId collectionId) {
        MongoClient mongoClient = clientFor(sourceConfig);
        return new SplitContext(
                mongoClient,
                collectionId,
                collStats(mongoClient, collectionId),
                sourceConfig.getSplitSize());
    }

    public MongoClient getMongoClient() {
        return mongoClient;
    }

    public TableId getCollectionId() {
        return collectionId;
    }

    public int getChunkSizeMB() {
        return chunkSizeMB;
    }

    /** The number of objects or documents in this collection. */
    public long getDocumentCount() {
        return collectionStats.getNumber("count", new BsonInt64(0)).longValue();
    }

    /** The total uncompressed size in memory of all records in a collection. */
    public long getSizeInBytes() {
        return collectionStats.getNumber("size", new BsonInt64(0)).longValue();
    }

    /** The average size of an object in the collection. */
    public long getAvgObjSizeInBytes() {
        return collectionStats.getNumber("avgObjSize", new BsonInt64(0)).longValue();
    }

    /** Is a sharded collection. */
    public boolean isShardedCollection() {
        return collectionStats.getBoolean("sharded", BsonBoolean.FALSE).getValue();
    }
}
