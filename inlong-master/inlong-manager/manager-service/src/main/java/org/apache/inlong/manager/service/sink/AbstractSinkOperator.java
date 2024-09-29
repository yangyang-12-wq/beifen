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

package org.apache.inlong.manager.service.sink;

import org.apache.inlong.common.pojo.sort.dataflow.sink.SinkConfig;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.SinkStatus;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.fieldtype.strategy.FieldTypeMappingStrategy;
import org.apache.inlong.manager.common.fieldtype.strategy.FieldTypeStrategyFactory;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.JsonUtils;
import org.apache.inlong.manager.dao.entity.StreamSinkEntity;
import org.apache.inlong.manager.dao.entity.StreamSinkFieldEntity;
import org.apache.inlong.manager.dao.mapper.InlongStreamEntityMapper;
import org.apache.inlong.manager.dao.mapper.SortConfigEntityMapper;
import org.apache.inlong.manager.dao.mapper.StreamSinkEntityMapper;
import org.apache.inlong.manager.dao.mapper.StreamSinkFieldEntityMapper;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.node.DataNodeInfo;
import org.apache.inlong.manager.pojo.sink.SinkField;
import org.apache.inlong.manager.pojo.sink.SinkRequest;
import org.apache.inlong.manager.pojo.sink.StreamSink;
import org.apache.inlong.manager.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.pojo.stream.StreamField;
import org.apache.inlong.manager.service.node.DataNodeOperateHelper;

import com.github.pagehelper.Page;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Default operation of stream sink.
 */
public abstract class AbstractSinkOperator implements StreamSinkOperator {

    protected static final String KEY_GROUP_ID = "inlongGroupId";
    protected static final String KEY_STREAM_ID = "inlongStreamId";
    protected static final String KEY_DATA_TYPE = "dataType";
    protected static final String KEY_SEPARATOR = "separator";
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSinkOperator.class);
    @Autowired
    protected StreamSinkEntityMapper sinkMapper;
    @Autowired
    protected StreamSinkFieldEntityMapper sinkFieldMapper;
    @Autowired
    protected DataNodeOperateHelper dataNodeHelper;
    @Autowired
    protected InlongStreamEntityMapper inlongStreamEntityMapper;
    @Autowired
    protected SortConfigEntityMapper sortConfigEntityMapper;
    @Autowired
    protected FieldTypeStrategyFactory fieldTypeStrategyFactory;

    /**
     * Setting the parameters of the latest entity.
     *
     * @param request sink request
     * @param targetEntity entity object which will set the new parameters.
     */
    protected abstract void setTargetEntity(SinkRequest request, StreamSinkEntity targetEntity);

    /**
     * Getting the sink type.
     *
     * @return sink type string.
     */
    protected abstract String getSinkType();

    @Override
    public Integer saveOpt(SinkRequest request, String operator) {
        StreamSinkEntity entity = CommonBeanUtils.copyProperties(request, StreamSinkEntity::new);
        entity.setStatus(SinkStatus.NEW.getCode());
        entity.setCreator(operator);
        entity.setModifier(operator);

        // get the ext params
        setTargetEntity(request, entity);
        sinkMapper.insert(entity);
        Integer sinkId = entity.getId();
        request.setId(sinkId);
        this.saveFieldOpt(request);
        return sinkId;
    }

    @Override
    public List<SinkField> getSinkFields(Integer sinkId) {
        List<StreamSinkFieldEntity> sinkFieldEntities = sinkFieldMapper.selectBySinkId(sinkId);
        return CommonBeanUtils.copyListProperties(sinkFieldEntities, SinkField::new);
    }

    @Override
    public PageResult<? extends StreamSink> getPageInfo(Page<StreamSinkEntity> entityPage) {
        if (CollectionUtils.isEmpty(entityPage)) {
            return PageResult.empty();
        }
        return PageResult.fromPage(entityPage).map(this::getFromEntity);
    }

    @Override
    public void updateOpt(SinkRequest request, SinkStatus nextStatus, String operator) {
        StreamSinkEntity entity = sinkMapper.selectByPrimaryKey(request.getId());
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.SINK_INFO_NOT_FOUND);
        }
        if (!Objects.equals(entity.getVersion(), request.getVersion())) {
            throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED,
                    String.format("sink has already updated with groupId=%s, streamId=%s, name=%s, curVersion=%s",
                            request.getInlongGroupId(), request.getInlongStreamId(), request.getSinkName(),
                            request.getVersion()));
        }
        CommonBeanUtils.copyProperties(request, entity, true);
        setTargetEntity(request, entity);
        entity.setPreviousStatus(entity.getStatus());
        if (nextStatus != null) {
            entity.setStatus(nextStatus.getCode());
        }
        entity.setModifier(operator);
        int rowCount = sinkMapper.updateByIdSelective(entity);
        if (rowCount != InlongConstants.AFFECTED_ONE_ROW) {
            throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED,
                    String.format("sink has already updated with groupId=%s, streamId=%s, name=%s, curVersion=%s",
                            request.getInlongGroupId(), request.getInlongStreamId(), request.getSinkName(),
                            request.getVersion()));
        }

        boolean onlyAdd = SinkStatus.CONFIG_SUCCESSFUL.getCode().equals(entity.getPreviousStatus());
        this.updateFieldOpt(onlyAdd, request);

        LOGGER.info("success to update sink of type={}", request.getSinkType());
    }

    @Override
    public void updateFieldOpt(Boolean onlyAdd, SinkRequest request) {
        Integer sinkId = request.getId();
        List<SinkField> fieldRequestList = request.getSinkFieldList();
        if (CollectionUtils.isEmpty(fieldRequestList)) {
            return;
        }
        if (onlyAdd) {
            List<StreamSinkFieldEntity> existsFieldList = sinkFieldMapper.selectBySinkId(sinkId);
            if (existsFieldList.size() > fieldRequestList.size()) {
                throw new BusinessException(ErrorCodeEnum.SINK_FIELD_UPDATE_NOT_ALLOWED);
            }
            for (int i = 0; i < existsFieldList.size(); i++) {
                if (!existsFieldList.get(i).getFieldName().equalsIgnoreCase(fieldRequestList.get(i).getFieldName())) {
                    throw new BusinessException(ErrorCodeEnum.SINK_FIELD_UPDATE_NOT_ALLOWED);
                }
            }
        }
        // First physically delete the existing fields
        sinkFieldMapper.deleteAll(sinkId);
        // Then batch save the sink fields
        this.saveFieldOpt(request);
        LOGGER.info("success to update sink field");
    }

    @Override
    public void saveFieldOpt(SinkRequest request) {
        List<SinkField> fieldList = request.getSinkFieldList();
        LOGGER.debug("begin to save sink fields={}", fieldList);
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
            StreamSinkFieldEntity fieldEntity = CommonBeanUtils.copyProperties(fieldInfo, StreamSinkFieldEntity::new);
            if (StringUtils.isEmpty(fieldEntity.getFieldComment())) {
                fieldEntity.setFieldComment(fieldEntity.getFieldName());
            }
            fieldEntity.setInlongGroupId(groupId);
            fieldEntity.setInlongStreamId(streamId);
            fieldEntity.setSinkType(sinkType);
            fieldEntity.setSinkId(sinkId);
            fieldEntity.setIsDeleted(InlongConstants.UN_DELETED);
            entityList.add(fieldEntity);
        }

        sinkFieldMapper.insertAll(entityList);
        LOGGER.debug("success to save sink fields");
    }

    @Override
    public void syncField(SinkRequest request, List<StreamField> streamFields) {
        FieldTypeMappingStrategy fieldTypeMappingStrategy = fieldTypeStrategyFactory.getInstance(request.getSinkType());
        if (fieldTypeMappingStrategy == null) {
            LOGGER.info("current sink type ={} not support sync field", request.getSinkType());
            return;
        }
        List<SinkField> sinkFields = request.getSinkFieldList();
        if (sinkFields.size() >= streamFields.size()) {
            return;
        }
        for (int i = sinkFields.size(); i < streamFields.size(); i++) {
            StreamField streamField = streamFields.get(i);
            SinkField sinkField = CommonBeanUtils.copyProperties(streamField, SinkField::new);
            sinkField.setSourceFieldName(streamField.getFieldName());
            sinkField.setSourceFieldType(streamField.getFieldType());
            sinkField.setFieldComment(streamField.getFieldComment());
            sinkField.setFieldName(streamField.getFieldName());
            sinkField.setFieldType(fieldTypeMappingStrategy.getStreamToSinkFieldTypeMapping(streamField.getFieldType())
                    .toLowerCase(Locale.ROOT));
            sinkFields.add(sinkField);
        }
        updateFieldOpt(true, request);
    }

    @Override
    public void deleteOpt(StreamSinkEntity entity, String operator) {
        sortConfigEntityMapper.logicDeleteBySinkId(entity.getId());
        entity.setPreviousStatus(entity.getStatus());
        entity.setStatus(InlongConstants.DELETED_STATUS);
        entity.setIsDeleted(entity.getId());
        entity.setModifier(operator);
        int rowCount = sinkMapper.updateByIdSelective(entity);
        if (rowCount != InlongConstants.AFFECTED_ONE_ROW) {
            throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED,
                    String.format("sink has already updated with groupId=%s, streamId=%s, name=%s, curVersion=%s",
                            entity.getInlongGroupId(), entity.getInlongStreamId(), entity.getSinkName(),
                            entity.getVersion()));
        }
        sinkFieldMapper.logicDeleteAll(entity.getId());
    }

    @Override
    public Map<String, String> parse2IdParams(StreamSinkEntity streamSink, List<String> fields,
            DataNodeInfo dataNodeInfo) {
        Map<String, String> param;
        try {
            HashMap<String, Object> streamInfoMap = JsonUtils.parseObject(streamSink.getExtParams(), HashMap.class);
            param = new HashMap<>();
            assert streamInfoMap != null;
            for (String key : streamInfoMap.keySet()) {
                param.put(key, String.valueOf(streamInfoMap.get(key)));
            }
            // put group and stream info
            param.put(KEY_GROUP_ID, streamSink.getInlongGroupId());
            param.put(KEY_STREAM_ID, streamSink.getInlongStreamId());
            return param;
        } catch (Exception e) {
            LOGGER.error(String.format(
                    "cannot parse properties for groupId=%s, streamId=%s, sinkName=%s, the row properties: %s",
                    streamSink.getInlongGroupId(), streamSink.getInlongStreamId(),
                    streamSink.getSinkName(), streamSink.getExtParams()),
                    e);

            return null;
        }
    }

    /**
     * Check the validity of sink fields.
     */
    protected void checkFieldInfo(SinkField fieldInfo) {

    }

    @Override
    public SinkConfig getSinkConfig(InlongGroupInfo groupInfo, InlongStreamInfo streamInfo, StreamSink sink) {
        throw new BusinessException(String.format("not support get sink config for sink type=%s", sink.getSinkType()));
    }

}
