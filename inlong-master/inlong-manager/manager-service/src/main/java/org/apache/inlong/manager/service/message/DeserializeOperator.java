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

package org.apache.inlong.manager.service.message;

import org.apache.inlong.common.enums.MessageWrapType;
import org.apache.inlong.common.pojo.sort.dataflow.deserialization.DeserializationConfig;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.pojo.consume.BriefMQMessage;
import org.apache.inlong.manager.pojo.consume.BriefMQMessage.FieldInfo;
import org.apache.inlong.manager.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.pojo.stream.QueryMessageRequest;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Deserialize of message operator
 */
public interface DeserializeOperator {

    String COMPRESS_TYPE_KEY = "compressType";
    String CLIENT_IP = "clientIp";
    String MSG_TIME_KEY = "msgTime";
    char INLONGMSG_ATTR_ENTRY_DELIMITER = '&';
    char INLONGMSG_ATTR_KV_DELIMITER = '=';

    // keys in attributes
    String INLONGMSG_ATTR_TIME_T = "t";
    String INLONGMSG_ATTR_TIME_DT = "dt";

    /**
     * Determines whether the current instance matches the specified type.
     */
    boolean accept(MessageWrapType type);

    /**
     * List brief mq message info
     *
     * @param streamInfo inlong stream info
     * @param msgBytes messages
     * @param headers message headers
     * @param index message index
     * @return list of brief mq message info
     */
    default List<BriefMQMessage> decodeMsg(InlongStreamInfo streamInfo, List<BriefMQMessage> briefMQMessages,
            byte[] msgBytes, Map<String, String> headers, int index, QueryMessageRequest request) throws Exception {
        return null;
    }

    default DeserializationConfig getDeserializationConfig(InlongStreamInfo streamInfo) {
        throw new BusinessException(String.format("current type not support DeserializationInfo for wrapType=%s",
                streamInfo.getWrapType()));
    }

    default Boolean checkIfFilter(QueryMessageRequest request, List<FieldInfo> streamFieldList) {
        if (StringUtils.isBlank(request.getFieldName()) || StringUtils.isBlank(request.getOperationType())
                || StringUtils.isBlank(request.getTargetValue())) {
            return false;
        }
        boolean ifFilter = false;
        FieldInfo fieldInfo = streamFieldList.stream()
                .filter(v -> Objects.equals(v.getFieldName(), request.getFieldName())).findFirst()
                .orElse(null);
        if (fieldInfo != null) {
            switch (request.getOperationType()) {
                case "=":
                    ifFilter = !Objects.equals(request.getTargetValue(), fieldInfo.getFieldValue());
                    break;
                case "!=":
                    ifFilter = Objects.equals(request.getTargetValue(), fieldInfo.getFieldValue());
                    break;
                case "like":
                    ifFilter = fieldInfo.getFieldValue() != null
                            && !fieldInfo.getFieldValue().contains(request.getTargetValue());
            }
        }
        return ifFilter;
    }
}
