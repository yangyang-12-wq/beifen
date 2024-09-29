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

package org.apache.inlong.manager.common.consts;

/**
 * Constant for stream types, indicating that both StreamSource and StreamSink support these types.
 */
public class StreamType {

    @SupportSortType(sortType = SortType.SORT_STANDALONE)
    public static final String KAFKA = "KAFKA";

    @SupportSortType(sortType = SortType.SORT_FLINK)
    public static final String HUDI = "HUDI";

    @SupportSortType(sortType = SortType.SORT_FLINK)
    public static final String POSTGRESQL = "POSTGRESQL";

    @SupportSortType(sortType = SortType.SORT_FLINK)
    public static final String SQLSERVER = "SQLSERVER";

    @SupportSortType(sortType = SortType.SORT_FLINK)
    public static final String ORACLE = "ORACLE";

    @SupportSortType(sortType = SortType.SORT_STANDALONE)
    public static final String PULSAR = "PULSAR";

    @SupportSortType(sortType = SortType.SORT_FLINK)
    public static final String ICEBERG = "ICEBERG";

}
