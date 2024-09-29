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

package org.apache.inlong.manager.service.sink.es;

import org.apache.inlong.common.pojo.sort.dataflow.field.FieldConfig;
import org.apache.inlong.common.pojo.sort.dataflow.field.format.FormatInfo;
import org.apache.inlong.common.pojo.sort.dataflow.sink.EsSinkConfig;
import org.apache.inlong.common.pojo.sort.dataflow.sink.SinkConfig;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.consts.SinkType;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.JsonUtils;
import org.apache.inlong.manager.dao.entity.InlongStreamEntity;
import org.apache.inlong.manager.dao.entity.StreamSinkEntity;
import org.apache.inlong.manager.dao.entity.StreamSinkFieldEntity;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.node.DataNodeInfo;
import org.apache.inlong.manager.pojo.sink.SinkField;
import org.apache.inlong.manager.pojo.sink.SinkRequest;
import org.apache.inlong.manager.pojo.sink.StreamSink;
import org.apache.inlong.manager.pojo.sink.es.ElasticsearchFieldInfo;
import org.apache.inlong.manager.pojo.sink.es.ElasticsearchSink;
import org.apache.inlong.manager.pojo.sink.es.ElasticsearchSinkDTO;
import org.apache.inlong.manager.pojo.sink.es.ElasticsearchSinkRequest;
import org.apache.inlong.manager.pojo.sort.util.FieldInfoUtils;
import org.apache.inlong.manager.pojo.stream.InlongStreamExtParam;
import org.apache.inlong.manager.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.service.sink.AbstractSinkOperator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Elasticsearch sink operator, such as save or update Elasticsearch field, etc.
 */
@Service
public class ElasticsearchSinkOperator extends AbstractSinkOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchSinkOperator.class);
    private static final String KEY_FIELDS = "fieldNames";

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Boolean accept(String sinkType) {
        return SinkType.ES.equals(sinkType);
    }

    @Override
    protected String getSinkType() {
        return SinkType.ES;
    }

    @Override
    protected void setTargetEntity(SinkRequest request, StreamSinkEntity targetEntity) {
        if (!this.getSinkType().equals(request.getSinkType())) {
            throw new BusinessException(ErrorCodeEnum.SINK_TYPE_NOT_SUPPORT,
                    ErrorCodeEnum.SINK_TYPE_NOT_SUPPORT.getMessage() + ": " + getSinkType());
        }
        ElasticsearchSinkRequest sinkRequest = (ElasticsearchSinkRequest) request;
        try {
            ElasticsearchSinkDTO dto = ElasticsearchSinkDTO.getFromRequest(sinkRequest, targetEntity.getExtParams());

            InlongStreamEntity stream = inlongStreamEntityMapper
                    .selectByIdentifier(request.getInlongGroupId(), request.getInlongStreamId());
            dto.setSeparator(String.valueOf((char) (Integer.parseInt(stream.getDataSeparator()))));

            InlongStreamExtParam streamExt =
                    JsonUtils.parseObject(stream.getExtParams(), InlongStreamExtParam.class);
            dto.setFieldOffset(streamExt.getExtendedFieldSize());

            targetEntity.setExtParams(objectMapper.writeValueAsString(dto));
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SINK_SAVE_FAILED,
                    String.format("serialize extParams of Elasticsearch SinkDTO failure: %s", e.getMessage()));
        }
    }

    @Override
    public StreamSink getFromEntity(StreamSinkEntity entity) {
        ElasticsearchSink sink = new ElasticsearchSink();
        if (entity == null) {
            return sink;
        }

        ElasticsearchSinkDTO dto = ElasticsearchSinkDTO.getFromJson(entity.getExtParams());
        CommonBeanUtils.copyProperties(entity, sink, true);
        CommonBeanUtils.copyProperties(dto, sink, true);
        List<SinkField> sinkFields = getSinkFields(entity.getId());
        sink.setSinkFieldList(sinkFields);
        return sink;
    }

    @Override
    public Map<String, String> parse2IdParams(StreamSinkEntity streamSink, List<String> fields,
            DataNodeInfo dataNodeInfo) {
        Map<String, String> idParams = super.parse2IdParams(streamSink, fields, dataNodeInfo);
        StringBuilder sb = new StringBuilder();
        for (String field : fields) {
            sb.append(field).append(" ");
        }
        idParams.computeIfAbsent(KEY_FIELDS, k -> sb.toString());
        return idParams;
    }

    @Override
    public void saveFieldOpt(SinkRequest request) {
        List<SinkField> fieldList = request.getSinkFieldList();
        LOGGER.debug("begin to save es sink fields={}", fieldList);
        if (CollectionUtils.isEmpty(fieldList)) {
            return;
        }

        int size = fieldList.size();
        List<StreamSinkFieldEntity> entityList = new ArrayList<>(size);
        String groupId = request.getInlongGroupId();
        String streamId = request.getInlongStreamId();
        String sinkType = request.getSinkType();
        Integer sinkId = request.getId();
        for (SinkField fieldInfo : fieldList) {
            this.checkFieldInfo(fieldInfo);
            fieldInfo.setExtParams(null);
            StreamSinkFieldEntity fieldEntity = CommonBeanUtils.copyProperties(fieldInfo, StreamSinkFieldEntity::new);
            if (StringUtils.isEmpty(fieldEntity.getFieldComment())) {
                fieldEntity.setFieldComment(fieldEntity.getFieldName());
            }
            try {
                ElasticsearchFieldInfo dto = ElasticsearchFieldInfo.getFromRequest(fieldInfo);
                fieldEntity.setExtParams(objectMapper.writeValueAsString(dto));
            } catch (Exception e) {
                throw new BusinessException(ErrorCodeEnum.SINK_SAVE_FAILED,
                        String.format("serialize extParams of Elasticsearch FieldInfo failure: %s", e.getMessage()));
            }
            fieldEntity.setInlongGroupId(groupId);
            fieldEntity.setInlongStreamId(streamId);
            fieldEntity.setSinkType(sinkType);
            fieldEntity.setSinkId(sinkId);
            fieldEntity.setIsDeleted(InlongConstants.UN_DELETED);
            entityList.add(fieldEntity);
        }

        sinkFieldMapper.insertAll(entityList);
        LOGGER.debug("success to save es sink fields");
    }

    @Override
    public List<SinkField> getSinkFields(Integer sinkId) {
        List<StreamSinkFieldEntity> sinkFieldEntities = sinkFieldMapper.selectBySinkId(sinkId);
        List<SinkField> fieldList = new ArrayList<>();
        if (CollectionUtils.isEmpty(sinkFieldEntities)) {
            return fieldList;
        }
        sinkFieldEntities.forEach(field -> {
            SinkField sinkField = new SinkField();
            if (StringUtils.isNotBlank(field.getExtParams())) {
                ElasticsearchFieldInfo elasticsearchFieldInfo = ElasticsearchFieldInfo.getFromJson(
                        field.getExtParams());
                CommonBeanUtils.copyProperties(field, elasticsearchFieldInfo, true);
                fieldList.add(elasticsearchFieldInfo);
            } else {
                CommonBeanUtils.copyProperties(field, sinkField, true);
                fieldList.add(sinkField);
            }

        });
        return fieldList;
    }

    @Override
    public SinkConfig getSinkConfig(InlongGroupInfo groupInfo, InlongStreamInfo streamInfo, StreamSink sink) {
        ElasticsearchSink elasticsearchSink = (ElasticsearchSink) sink;
        StreamSinkEntity streamSinkEntity = sinkMapper.selectByPrimaryKey(sink.getId());
        ElasticsearchSinkDTO elasticsearchSinkDTO = ElasticsearchSinkDTO.getFromJson(streamSinkEntity.getExtParams());
        EsSinkConfig sinkConfig = CommonBeanUtils.copyProperties(elasticsearchSink, EsSinkConfig::new);
        CommonBeanUtils.copyProperties(elasticsearchSinkDTO, sinkConfig);
        sinkConfig.setSeparator(String.valueOf((char) (Integer.parseInt(streamInfo.getDataSeparator()))));
        sinkConfig.setFieldOffset(streamInfo.getExtendedFieldSize());
        sinkConfig.setContentOffset(0);
        List<FieldConfig> fields = sinkFieldMapper.selectBySinkId(sink.getId()).stream().map(
                v -> {
                    FieldConfig fieldConfig = new FieldConfig();
                    FormatInfo formatInfo = FieldInfoUtils.convertFieldFormat(
                            v.getFieldType().toLowerCase());
                    fieldConfig.setName(v.getFieldName());
                    fieldConfig.setFormatInfo(formatInfo);
                    return fieldConfig;
                }).collect(Collectors.toList());
        sinkConfig.setFieldConfigs(fields);
        return sinkConfig;
    }

}
