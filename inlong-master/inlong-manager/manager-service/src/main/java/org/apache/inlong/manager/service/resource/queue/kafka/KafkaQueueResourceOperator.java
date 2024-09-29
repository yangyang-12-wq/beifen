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

package org.apache.inlong.manager.service.resource.queue.kafka;

import org.apache.inlong.common.constant.Constants;
import org.apache.inlong.common.constant.MQType;
import org.apache.inlong.manager.common.enums.ClusterType;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.WorkflowListenerException;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.InlongStreamEntity;
import org.apache.inlong.manager.dao.entity.StreamSinkEntity;
import org.apache.inlong.manager.pojo.cluster.ClusterInfo;
import org.apache.inlong.manager.pojo.cluster.kafka.KafkaClusterInfo;
import org.apache.inlong.manager.pojo.consume.BriefMQMessage;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.group.kafka.InlongKafkaInfo;
import org.apache.inlong.manager.pojo.stream.InlongStreamBriefInfo;
import org.apache.inlong.manager.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.pojo.stream.QueryMessageRequest;
import org.apache.inlong.manager.service.cluster.InlongClusterService;
import org.apache.inlong.manager.service.consume.InlongConsumeService;
import org.apache.inlong.manager.service.resource.queue.QueueResourceOperator;
import org.apache.inlong.manager.service.stream.InlongStreamService;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import java.util.List;

/**
 * Operator for creating Kafka Topic and Subscription
 */
@Slf4j
@Service
public class KafkaQueueResourceOperator implements QueueResourceOperator {

    /**
     * The name rule for Kafka consumer group: clusterTag_topicName_consumer_group
     */
    public static final String KAFKA_CONSUMER_GROUP = "%s_%s_consumer_group";

    public static final String KAFKA_CONSUMER_GROUP_REALTIME_REVIEW = "%s_%s_consumer_group_realtime_review";

    @Autowired
    private KafkaOperator kafkaOperator;
    @Autowired
    private InlongStreamService streamService;
    @Autowired
    private InlongConsumeService consumeService;
    @Autowired
    private InlongClusterService clusterService;

    @Override
    public boolean accept(String mqType) {
        return MQType.KAFKA.equals(mqType);
    }

    @Override
    public void createQueueForGroup(@NotNull InlongGroupInfo groupInfo, @NotBlank String operator) {
        log.info("skip to create kafka topic for groupId={}, just create in each inlong stream",
                groupInfo.getInlongGroupId());
    }

    @Override
    public void deleteQueueForGroup(InlongGroupInfo groupInfo, String operator) {
        Preconditions.expectNotNull(groupInfo, "inlong group info cannot be null");

        String groupId = groupInfo.getInlongGroupId();
        log.info("begin to delete kafka resource for groupId={}", groupId);
        ClusterInfo clusterInfo = clusterService.getOne(groupInfo.getInlongClusterTag(), null, ClusterType.KAFKA);
        try {
            List<InlongStreamBriefInfo> streamInfoList = streamService.getTopicList(groupId);
            if (streamInfoList == null || streamInfoList.isEmpty()) {
                log.warn("skip to create kafka topic and subscription as no streams for groupId={}", groupId);
                return;
            }
            for (InlongStreamBriefInfo streamInfo : streamInfoList) {
                this.deleteKafkaTopic(groupInfo, streamInfo.getMqResource());
            }
        } catch (Exception e) {
            log.error("failed to delete kafka resource for groupId=" + groupId, e);
            throw new WorkflowListenerException("failed to delete kafka resource: " + e.getMessage());
        }
        log.info("success to delete kafka resource for groupId={}, cluster={}", groupId, clusterInfo);
    }

    @Override
    public void createQueueForStream(InlongGroupInfo groupInfo, InlongStreamInfo streamInfo, String operator) {
        Preconditions.expectNotNull(groupInfo, "inlong group info cannot be null");
        Preconditions.expectNotNull(streamInfo, "inlong stream info cannot be null");
        Preconditions.expectNotBlank(operator, ErrorCodeEnum.INVALID_PARAMETER, "operator cannot be null");

        String groupId = streamInfo.getInlongGroupId();
        String streamId = streamInfo.getInlongStreamId();
        log.info("begin to create kafka resource for groupId={}, streamId={}", groupId, streamId);

        try {
            InlongKafkaInfo inlongKafkaInfo = (InlongKafkaInfo) groupInfo;
            // create kafka topic
            String topicName = streamInfo.getMqResource();
            if (topicName.equals(streamId)) {
                // the default mq resource (stream id) is not sufficient to discriminate different kafka topics
                topicName = String.format(Constants.DEFAULT_KAFKA_TOPIC_FORMAT,
                        inlongKafkaInfo.getMqResource(), streamInfo.getMqResource());
            }
            this.createKafkaTopic(inlongKafkaInfo, topicName);
        } catch (Exception e) {
            String msg = String.format("failed to create kafka topic for groupId=%s, streamId=%s", groupId, streamId);
            log.error(msg, e);
            throw new WorkflowListenerException(msg + ": " + e.getMessage());
        }
        log.info("success to create kafka resource for groupId={}, streamId={}", groupId, streamId);
    }

    @Override
    public void deleteQueueForStream(InlongGroupInfo groupInfo, InlongStreamInfo streamInfo, String operator) {
        Preconditions.expectNotNull(groupInfo, "inlong group info cannot be null");
        Preconditions.expectNotNull(streamInfo, "inlong stream info cannot be null");

        String groupId = streamInfo.getInlongGroupId();
        String streamId = streamInfo.getInlongStreamId();
        log.info("begin to delete kafka resource for groupId={} streamId={}", groupId, streamId);

        try {
            String topicName = streamInfo.getMqResource();
            if (StringUtils.isBlank(topicName) || topicName.equals(streamId)) {
                // the default mq resource (stream id) is not sufficient to discriminate different kafka topics
                topicName = String.format(Constants.DEFAULT_KAFKA_TOPIC_FORMAT,
                        groupInfo.getMqResource(), streamInfo.getMqResource());
            }
            this.deleteKafkaTopic(groupInfo, topicName);
            log.info("success to delete kafka topic for groupId={}, streamId={}", groupId, streamId);
        } catch (Exception e) {
            String msg = String.format("failed to delete kafka topic for groupId=%s, streamId=%s", groupId, streamId);
            log.error(msg, e);
            throw new WorkflowListenerException(msg);
        }
        log.info("success to delete kafka resource for groupId={}, streamId={}", groupId, streamId);
    }

    /**
     * Create Kafka Topic and Subscription, and save the consumer group info.
     */
    private void createKafkaTopic(InlongKafkaInfo kafkaInfo, String topicName) throws Exception {
        // create Kafka topic
        ClusterInfo clusterInfo = clusterService.getOne(kafkaInfo.getInlongClusterTag(), null, ClusterType.KAFKA);
        kafkaOperator.createTopic(kafkaInfo, (KafkaClusterInfo) clusterInfo, topicName);

        boolean exist = kafkaOperator.topicIsExists((KafkaClusterInfo) clusterInfo, topicName);
        if (!exist) {
            String bootStrapServers = clusterInfo.getUrl();
            log.error("topic={} not exists in {}", topicName, bootStrapServers);
            throw new WorkflowListenerException("topic=" + topicName + " not exists in " + bootStrapServers);
        }

        // Kafka consumers do not need to register in advance

        // save the consumer group info for the Kafka topic
        String consumeGroup = String.format(KAFKA_CONSUMER_GROUP, kafkaInfo.getInlongClusterTag(), topicName);
        Integer id = consumeService.saveBySystem(kafkaInfo, topicName, consumeGroup);
        log.info("success to save inlong consume [{}] for consumerGroup={}, groupId={}, topic={}",
                id, consumeGroup, kafkaInfo.getInlongGroupId(), topicName);
    }

    /**
     * Delete Kafka Topic and Subscription, and delete the consumer group info.
     */
    private void deleteKafkaTopic(InlongGroupInfo groupInfo, String topicName) {
        ClusterInfo clusterInfo = clusterService.getOne(groupInfo.getInlongClusterTag(), null, ClusterType.KAFKA);
        kafkaOperator.forceDeleteTopic((KafkaClusterInfo) clusterInfo, topicName);
    }

    @Override
    public List<BriefMQMessage> queryLatestMessages(InlongGroupInfo groupInfo, InlongStreamInfo streamInfo,
            QueryMessageRequest request) {
        ClusterInfo clusterInfo = clusterService.getOne(groupInfo.getInlongClusterTag(), null, ClusterType.KAFKA);

        String topicName = streamInfo.getMqResource();
        if (topicName.equals(streamInfo.getInlongStreamId())) {
            // the default mq resource (stream id) is not sufficient to discriminate different kafka topics
            topicName = String.format(Constants.DEFAULT_KAFKA_TOPIC_FORMAT,
                    groupInfo.getMqResource(), streamInfo.getMqResource());
        }

        String consumeGroup =
                String.format(KAFKA_CONSUMER_GROUP_REALTIME_REVIEW, groupInfo.getInlongClusterTag(), topicName);
        return kafkaOperator.queryLatestMessage((KafkaClusterInfo) clusterInfo, topicName, consumeGroup, streamInfo,
                request);
    }

    @Override
    public String getSortConsumeGroup(InlongGroupInfo groupInfo, InlongStreamEntity streamEntity,
            StreamSinkEntity sinkEntity) {
        InlongKafkaInfo kafkaInfo = (InlongKafkaInfo) groupInfo;
        String topicName = streamEntity.getMqResource();
        return String.format(KAFKA_CONSUMER_GROUP, kafkaInfo.getInlongClusterTag(), topicName);
    }
}
