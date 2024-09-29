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

package org.apache.inlong.manager.dao.mapper;

import org.apache.inlong.manager.dao.entity.StreamSourceEntity;
import org.apache.inlong.manager.pojo.source.SourcePageRequest;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StreamSourceEntityMapper {

    int insert(StreamSourceEntity record);

    StreamSourceEntity selectById(Integer id);

    StreamSourceEntity selectByIdForUpdate(Integer id);

    /**
     * Only used for agent collector, which will select all tasks related include deleted tasks.
     *
     * @param id stream source id
     * @return stream source info
     */
    StreamSourceEntity selectForAgentTask(Integer id);

    /**
     * Select one data add task by task map id and agent ip.
     *
     * @param taskMapId template id
     * @param agentIp agent ip
     * @return stream source info
     */
    StreamSourceEntity selectOneByTaskMapIdAndAgentIp(@Param("taskMapId") Integer taskMapId,
            @Param("agentIp") String agentIp);

    /**
     * Query un-deleted sources by the given agentIp.
     */
    List<StreamSourceEntity> selectByAgentIp(@Param("agentIp") String agentIp);

    /**
     * According to the inlong group id and inlong stream id, query the number of valid source
     */
    int selectCount(@Param("groupId") String groupId, @Param("streamId") String streamId);

    /**
     * According to the inlong group id and inlong stream id, query the number of data add task
     */
    int selectDataAddTaskCount(@Param("groupId") String groupId, @Param("streamId") String streamId);

    List<StreamSourceEntity> selectByByTimeout(@Param("retentionDays") Integer retentionDays);

    /**
     * Paging query source list based on conditions
     */
    List<StreamSourceEntity> selectByCondition(@Param("request") SourcePageRequest request);

    /**
     * Query valid source list by the given group id, stream id and source name.
     */
    List<StreamSourceEntity> selectByRelatedId(@Param("groupId") String groupId, @Param("streamId") String streamId,
            @Param("sourceName") String sourceName);

    /**
     * Query the tasks by the given status list.
     */
    List<StreamSourceEntity> selectByStatus(@Param("statusList") List<Integer> list, @Param("limit") int limit);

    /**
     * Query the tasks by the given status list and type List.
     */
    List<StreamSourceEntity> selectByStatusAndType(@Param("statusList") List<Integer> statusList,
            @Param("sourceTypeList") List<String> sourceTypeList, @Param("limit") int limit);

    /**
     * Query the tasks by the given status list and type List.
     */
    List<StreamSourceEntity> selectByAgentIpAndCluster(@Param("statusList") List<Integer> statusList,
            @Param("sourceTypeList") List<String> sourceTypeList, @Param("agentIp") String agentIp,
            @Param("clusterName") String clusterName);

    /**
     * Query the template tasks by the given status list and type List and clusterName.
     */
    List<StreamSourceEntity> selectTemplateSourceByCluster(@Param("statusList") List<Integer> statusList,
            @Param("sourceTypeList") List<String> sourceTypeList, @Param("clusterName") String clusterName);

    /**
     * Query the sources by the given status and Agent cluster info.
     *
     * @apiNote Sources with is_deleted > 0 should also be returned to agents to clear their local tasks.
     */
    List<StreamSourceEntity> selectByStatusAndCluster(@Param("statusList") List<Integer> statusList,
            @Param("clusterName") String clusterName, @Param("agentIp") String agentIp, @Param("uuid") String uuid);

    /**
     * Select all sources by groupIds
     */
    List<StreamSourceEntity> selectByGroupIds(@Param("groupIdList") List<String> groupIdList);

    /**
     * Select all data add task by task map id
     */
    List<StreamSourceEntity> selectByTaskMapId(@Param("taskMapId") Integer taskMapId);

    /**
     * Get the distinct source type from the given groupId and streamId
     */
    List<String> selectSourceType(@Param("groupId") String groupId, @Param("streamId") String streamId);

    /**
     * Query need update source according to the dataNodeName , clusterName, sourceType
     */
    List<Integer> selectNeedUpdateIdsByClusterAndDataNode(@Param("clusterName") String clusterName,
            @Param("nodeName") String nodeName, @Param("sourceType") String sourceType);

    /**
     * Query need update tasks by the given status list and type List.
     */
    List<Integer> selectHeartbeatTimeoutIds(@Param("sourceTypeList") List<String> sourceTypeList,
            @Param("agentIp") String agentIp,
            @Param("clusterName") String clusterName);

    int updateByPrimaryKeySelective(StreamSourceEntity record);

    int updateByPrimaryKey(StreamSourceEntity record);

    /**
     * Update the status to `nextStatus` by the given id.
     */
    int updateStatus(@Param("id") Integer id, @Param("nextStatus") Integer nextStatus,
            @Param("changeTime") Boolean changeModifyTime);

    /**
     * Update the status to `nextStatus` by the given group id and stream id.
     *
     * @apiNote Should not change the modify_time
     */
    int updateStatusByRelatedId(@Param("groupId") String groupId, @Param("streamId") String streamId,
            @Param("nextStatus") Integer nextStatus);

    /**
     * Update the agentIp and uuid.
     */
    int updateIpAndUuid(@Param("id") Integer id, @Param("agentIp") String agentIp, @Param("uuid") String uuid,
            @Param("changeTime") Boolean changeModifyTime);

    int updateSnapshot(StreamSourceEntity entity);

    /**
     * Update the source status
     *
     * @param idList source id list
     * @param status modify the status to this
     * @param operator operator name
     */
    void updateStatusByIds(@Param("idList") List<Integer> idList, @Param("status") Integer status,
            @Param("operator") String operator);

    /**
     * Update the source status
     *
     * @param idList source id list
     * @param operator operator name
     */
    void rollbackTimeoutStatusByIds(@Param("idList") List<Integer> idList, @Param("operator") String operator);

    /**
     * Update the source status when it has been deleted
     *
     * @param beforeSeconds the modified time was beforeSeconds seconds ago
     */
    void updateStatusToTimeout(@Param("beforeSeconds") Integer beforeSeconds);

    /**
     * Update the source status when it has been deleted
     *
     */
    void updateStatusByDeleted();

    /**
     * Logic delete the data add task by modifiy time
     */
    void logicalDeleteByTimeout(@Param("retentionDays") Integer retentionDays);

    int logicalDeleteByRelatedId(@Param("groupId") String groupId, @Param("streamId") String streamId,
            @Param("status") Integer status);

    int logicalDeleteByIds(@Param("idList") List<Integer> idList, @Param("status") Integer status);

    /**
     * Logical delete stream source by agentIp, change status at same time.
     *
     * @param agentIp ip of agent cluster node
     * @param status  status to change
     * @param targetStatus status of stream source now
     *
     */
    void logicalDeleteByAgentIp(@Param("agentIp") String agentIp, @Param("status") Integer status,
            @Param("targetStatus") Integer targetStatus);

    /**
     * Physical delete stream sources by group id and stream id
     */
    int deleteByRelatedId(@Param("groupId") String groupId, @Param("streamId") String streamId);

    /**
     * Physically delete all stream sources based on inlong group ids
     *
     * @return rows deleted
     */
    int deleteByInlongGroupIds(@Param("groupIdList") List<String> groupIdList);

}
