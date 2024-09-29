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

package org.apache.inlong.manager.pojo.sort.node.provider;

import org.apache.inlong.manager.common.consts.SinkType;
import org.apache.inlong.manager.pojo.sink.greenplum.GreenplumSink;
import org.apache.inlong.manager.pojo.sort.node.base.LoadNodeProvider;
import org.apache.inlong.manager.pojo.stream.StreamField;
import org.apache.inlong.manager.pojo.stream.StreamNode;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.node.LoadNode;
import org.apache.inlong.sort.protocol.node.load.GreenplumLoadNode;
import org.apache.inlong.sort.protocol.transformation.FieldRelation;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * The Provider for creating Greenplum load nodes.
 */
@Service
public class GreenplumProvider implements LoadNodeProvider {

    @Override
    public Boolean accept(String sinkType) {
        return SinkType.GREENPLUM.equals(sinkType);
    }

    @Override
    public LoadNode createLoadNode(StreamNode nodeInfo, Map<String, StreamField> constantFieldMap) {
        GreenplumSink greenplumSink = (GreenplumSink) nodeInfo;
        Map<String, String> properties = parseProperties(greenplumSink.getProperties());
        List<FieldInfo> fieldInfos = parseSinkFieldInfos(greenplumSink.getSinkFieldList(), greenplumSink.getSinkName());
        List<FieldRelation> fieldRelations = parseSinkFields(greenplumSink.getSinkFieldList(), constantFieldMap);
        return new GreenplumLoadNode(
                greenplumSink.getSinkName(),
                greenplumSink.getSinkName(),
                fieldInfos,
                fieldRelations,
                null,
                null,
                null,
                properties,
                greenplumSink.getJdbcUrl(),
                greenplumSink.getUsername(),
                greenplumSink.getPassword(),
                greenplumSink.getTableName(),
                greenplumSink.getPrimaryKey());
    }
}