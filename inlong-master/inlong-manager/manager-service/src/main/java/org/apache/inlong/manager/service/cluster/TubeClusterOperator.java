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

package org.apache.inlong.manager.service.cluster;

import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.enums.ClusterType;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.HttpUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.InlongClusterEntity;
import org.apache.inlong.manager.pojo.cluster.ClusterInfo;
import org.apache.inlong.manager.pojo.cluster.ClusterRequest;
import org.apache.inlong.manager.pojo.cluster.tubemq.TubeClusterDTO;
import org.apache.inlong.manager.pojo.cluster.tubemq.TubeClusterInfo;
import org.apache.inlong.manager.pojo.cluster.tubemq.TubeClusterRequest;
import org.apache.inlong.manager.service.group.InlongGroupOperator4NoneMQ;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * TubeMQ cluster operator.
 */
@Service
public class TubeClusterOperator extends AbstractClusterOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(InlongGroupOperator4NoneMQ.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Boolean accept(String clusterType) {
        return getClusterType().equals(clusterType);
    }

    @Override
    public String getClusterType() {
        return ClusterType.TUBEMQ;
    }

    @Override
    protected void setTargetEntity(ClusterRequest request, InlongClusterEntity targetEntity) {
        TubeClusterRequest tubeRequest = (TubeClusterRequest) request;
        CommonBeanUtils.copyProperties(tubeRequest, targetEntity, true);
        try {
            TubeClusterDTO dto = TubeClusterDTO.getFromRequest(tubeRequest, targetEntity.getExtParams());
            dto.setMasterIpPortList(request.getUrl());
            targetEntity.setExtParams(objectMapper.writeValueAsString(dto));
            LOGGER.debug("success to set entity for tubemq cluster");
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.CLUSTER_INFO_INCORRECT,
                    String.format("serialize extParams of TubeMQ Cluster failure: %s", e.getMessage()));
        }
    }

    @Override
    public ClusterInfo getFromEntity(InlongClusterEntity entity) {
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.CLUSTER_NOT_FOUND);
        }
        TubeClusterInfo tubeClusterInfo = new TubeClusterInfo();
        CommonBeanUtils.copyProperties(entity, tubeClusterInfo);
        if (StringUtils.isNotBlank(entity.getExtParams())) {
            TubeClusterDTO dto = TubeClusterDTO.getFromJson(entity.getExtParams());
            CommonBeanUtils.copyProperties(dto, tubeClusterInfo);
        }

        LOGGER.debug("success to get tubemq cluster info from entity");
        return tubeClusterInfo;
    }

    @Override
    public Boolean testConnection(ClusterRequest request) {
        String masterUrl = request.getUrl();
        int hostBeginIndex = masterUrl.lastIndexOf(InlongConstants.SLASH);
        int portBeginIndex = masterUrl.lastIndexOf(InlongConstants.COLON);
        String host = masterUrl.substring(hostBeginIndex + 1, portBeginIndex);
        int port = Integer.parseInt(masterUrl.substring(portBeginIndex + 1));
        Preconditions.expectNotBlank(masterUrl, ErrorCodeEnum.INVALID_PARAMETER, "connection url cannot be empty");
        boolean result;
        try {
            result = HttpUtils.checkConnectivity(host, port, 10, TimeUnit.SECONDS);
            LOGGER.debug("tube connection not null - connection success for masterUrl={}", masterUrl);
            return result;
        } catch (Exception e) {
            String errMsg = String.format("tube connection failed for masterUrl=%s", masterUrl);
            LOGGER.error(errMsg, e);
            throw new BusinessException(errMsg);
        }
    }

    @Override
    public Object getClusterInfo(InlongClusterEntity entity) {
        TubeClusterInfo tubeClusterInfo = (TubeClusterInfo) this.getFromEntity(entity);
        Map<String, String> map = new HashMap<>();
        map.put("RPC Url", tubeClusterInfo.getUrl());
        map.put("Web url", tubeClusterInfo.getMasterWebUrl());
        return map;
    }

}
