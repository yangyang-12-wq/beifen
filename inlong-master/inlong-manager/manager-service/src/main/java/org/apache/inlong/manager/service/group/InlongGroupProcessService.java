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

package org.apache.inlong.manager.service.group;

import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.GroupOperateType;
import org.apache.inlong.manager.common.enums.GroupStatus;
import org.apache.inlong.manager.common.enums.ProcessName;
import org.apache.inlong.manager.common.enums.TaskStatus;
import org.apache.inlong.manager.common.enums.TenantUserTypeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.exceptions.WorkflowListenerException;
import org.apache.inlong.manager.common.threadPool.VisiableThreadPoolTaskExecutor;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.InlongGroupEntity;
import org.apache.inlong.manager.dao.entity.WorkflowProcessEntity;
import org.apache.inlong.manager.dao.mapper.InlongGroupEntityMapper;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.group.InlongGroupResetRequest;
import org.apache.inlong.manager.pojo.stream.InlongStreamBriefInfo;
import org.apache.inlong.manager.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.pojo.user.LoginUserUtils;
import org.apache.inlong.manager.pojo.user.UserInfo;
import org.apache.inlong.manager.pojo.workflow.ProcessRequest;
import org.apache.inlong.manager.pojo.workflow.TaskResponse;
import org.apache.inlong.manager.pojo.workflow.WorkflowResult;
import org.apache.inlong.manager.pojo.workflow.form.process.ApplyGroupProcessForm;
import org.apache.inlong.manager.pojo.workflow.form.process.ApplyGroupProcessForm.GroupFullInfo;
import org.apache.inlong.manager.pojo.workflow.form.process.GroupResourceProcessForm;
import org.apache.inlong.manager.service.stream.InlongStreamService;
import org.apache.inlong.manager.service.workflow.WorkflowService;
import org.apache.inlong.manager.workflow.core.WorkflowQueryService;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;

import static org.apache.inlong.manager.common.consts.InlongConstants.ALIVE_TIME_MS;
import static org.apache.inlong.manager.common.consts.InlongConstants.CORE_POOL_SIZE;
import static org.apache.inlong.manager.common.consts.InlongConstants.MAX_POOL_SIZE;
import static org.apache.inlong.manager.common.consts.InlongConstants.QUEUE_SIZE;

/**
 * Operation to the inlong group process
 */
@Service
public class InlongGroupProcessService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InlongGroupProcessService.class);

    private static final ExecutorService EXECUTOR_SERVICE = new VisiableThreadPoolTaskExecutor(
            CORE_POOL_SIZE,
            MAX_POOL_SIZE,
            ALIVE_TIME_MS,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(QUEUE_SIZE),
            new ThreadFactoryBuilder().setNameFormat("inlong-group-process-%s").build(),
            new CallerRunsPolicy());

    @Autowired
    private InlongGroupEntityMapper groupMapper;
    @Autowired
    private InlongGroupService groupService;
    @Autowired
    private WorkflowQueryService workflowQueryService;
    @Autowired
    private WorkflowService workflowService;
    @Autowired
    private InlongStreamService streamService;

    /**
     * Start a New InlongGroup for the specified inlong group id.
     *
     * @param groupId inlong group id
     * @param operator name of operator
     * @return workflow result
     */
    public WorkflowResult startProcess(String groupId, String operator) {
        LOGGER.info("begin to start approve process for groupId={} by operator={}", groupId, operator);

        groupService.updateStatus(groupId, GroupStatus.TO_BE_APPROVAL.getCode(), operator);
        ApplyGroupProcessForm form = genApplyGroupProcessForm(groupId);
        WorkflowResult result = workflowService.start(ProcessName.APPLY_GROUP_PROCESS, operator, form);
        List<TaskResponse> tasks = result.getNewTasks();
        if (TaskStatus.FAILED == tasks.get(tasks.size() - 1).getStatus()) {
            throw new BusinessException(ErrorCodeEnum.WORKFLOW_START_RECORD_FAILED,
                    String.format("failed to start inlong group for groupId=%s", groupId));
        }

        LOGGER.info("success to start approve process for groupId={} by operator={}", groupId, operator);
        return result;
    }

    public WorkflowResult batchStartProcess(List<String> groupIdList, String operator) {
        for (String groupId : groupIdList) {
            LOGGER.info("begin to start approve process for groupId={} by operator={}", groupId, operator);

            groupService.updateStatus(groupId, GroupStatus.TO_BE_APPROVAL.getCode(), operator);
        }
        ApplyGroupProcessForm form = genApplyGroupProcessForm(groupIdList);
        WorkflowResult result = workflowService.start(ProcessName.APPLY_GROUP_PROCESS, operator, form);
        List<TaskResponse> tasks = result.getNewTasks();
        if (TaskStatus.FAILED == tasks.get(tasks.size() - 1).getStatus()) {
            throw new BusinessException(ErrorCodeEnum.WORKFLOW_START_RECORD_FAILED,
                    String.format("failed to start inlong group for groupId=%s", groupIdList));
        }

        LOGGER.info("success to start approve process for groupId={} by operator={}", groupIdList, operator);
        return result;
    }

    /**
     * Suspend InlongGroup in an asynchronous way.
     *
     * @param groupId inlong group id
     * @param operator name of operator
     * @return inlong group id
     * @apiNote Stop source and sort task related to the inlong group asynchronously, persist the status if
     *         necessary.
     */
    public String suspendProcessAsync(String groupId, String operator) {
        LOGGER.info("begin to suspend process asynchronously for groupId={} by operator={}", groupId, operator);

        groupService.updateStatus(groupId, GroupStatus.CONFIG_OFFLINE_ING.getCode(), operator);
        InlongGroupInfo groupInfo = groupService.get(groupId);
        GroupResourceProcessForm form = genGroupResourceProcessForm(groupInfo, GroupOperateType.SUSPEND);
        UserInfo userInfo = LoginUserUtils.getLoginUser();
        EXECUTOR_SERVICE.execute(
                () -> workflowService.startAsync(ProcessName.SUSPEND_GROUP_PROCESS, userInfo, form));

        LOGGER.info("success to suspend process asynchronously for groupId={} by operator={}", groupId, operator);
        return groupId;
    }

    /**
     * Suspend InlongGroup which is started up successfully.
     *
     * @param groupId inlong group id
     * @param operator name of operator
     * @return workflow result
     * @apiNote Stop source and sort task related to the inlong group asynchronously, persist the status if
     *         necessary.
     */
    public WorkflowResult suspendProcess(String groupId, String operator) {
        LOGGER.info("begin to suspend process for groupId={} by operator={}", groupId, operator);

        groupService.updateStatus(groupId, GroupStatus.CONFIG_OFFLINE_ING.getCode(), operator);
        InlongGroupInfo groupInfo = groupService.get(groupId);
        GroupResourceProcessForm form = genGroupResourceProcessForm(groupInfo, GroupOperateType.SUSPEND);
        WorkflowResult result = workflowService.start(ProcessName.SUSPEND_GROUP_PROCESS, operator, form);
        List<TaskResponse> tasks = result.getNewTasks();
        if (TaskStatus.FAILED == tasks.get(tasks.size() - 1).getStatus()) {
            throw new BusinessException(ErrorCodeEnum.WORKFLOW_SUSPEND_RECORD_FAILED,
                    String.format("failed to suspend inlong group for groupId=%s", groupId));
        }

        LOGGER.info("success to suspend process for groupId={} by operator={}", groupId, operator);
        return result;
    }

    /**
     * Restart InlongGroup in an asynchronous way, starting from the last persist snapshot.
     *
     * @param groupId inlong group id
     * @param operator name of operator
     * @return workflow result
     */
    public String restartProcessAsync(String groupId, String operator) {
        LOGGER.info("begin to restart process asynchronously for groupId={} by operator={}", groupId, operator);

        groupService.updateStatus(groupId, GroupStatus.CONFIG_ONLINE_ING.getCode(), operator);
        InlongGroupInfo groupInfo = groupService.get(groupId);
        GroupResourceProcessForm form = genGroupResourceProcessForm(groupInfo, GroupOperateType.RESTART);
        UserInfo userInfo = LoginUserUtils.getLoginUser();
        EXECUTOR_SERVICE.execute(
                () -> workflowService.startAsync(ProcessName.RESTART_GROUP_PROCESS, userInfo, form));

        LOGGER.info("success to restart process asynchronously for groupId={} by operator={}", groupId, operator);
        return groupId;
    }

    /**
     * Restart InlongGroup which is started up successfully, starting from the last persist snapshot.
     *
     * @param groupId inlong group id
     * @param operator name of operator
     * @return workflow result
     */
    public WorkflowResult restartProcess(String groupId, String operator) {
        LOGGER.info("begin to restart process for groupId={} by operator={}", groupId, operator);

        groupService.updateStatus(groupId, GroupStatus.CONFIG_ONLINE_ING.getCode(), operator);
        InlongGroupInfo groupInfo = groupService.get(groupId);
        GroupResourceProcessForm form = genGroupResourceProcessForm(groupInfo, GroupOperateType.RESTART);
        WorkflowResult result = workflowService.start(ProcessName.RESTART_GROUP_PROCESS, operator, form);
        List<TaskResponse> tasks = result.getNewTasks();
        if (TaskStatus.FAILED == tasks.get(tasks.size() - 1).getStatus()) {
            throw new BusinessException(ErrorCodeEnum.WORKFLOW_RESTART_RECORD_FAILED,
                    String.format("failed to restart inlong group for groupId=%s", groupId));
        }

        LOGGER.info("success to restart process for groupId={} by operator={}", groupId, operator);
        return result;
    }

    /**
     * Delete InlongGroup logically and delete related resource in an asynchronous way.
     *
     * @param groupId inlong group id
     * @param operator name of operator
     * @return inlong group id
     */
    public String deleteProcessAsync(String groupId, String operator) {
        LOGGER.info("begin to delete group asynchronously for groupId={} by user={}", groupId, operator);
        EXECUTOR_SERVICE.execute(() -> {
            try {
                invokeDeleteProcess(groupId, operator);
            } catch (Exception e) {
                LOGGER.error(String.format("failed to async delete group for groupId=%s by %s", groupId, operator), e);
                throw e;
            }
        });

        LOGGER.info("success to delete group asynchronously for groupId={} by user={}", groupId, operator);
        return groupId;
    }

    /**
     * Delete InlongGroup logically and delete related resource in a synchronous way.
     */
    public Boolean deleteProcess(String groupId, String operator) {
        LOGGER.info("begin to delete group for groupId={} by user={}", groupId, operator);
        try {
            invokeDeleteProcess(groupId, operator);
        } catch (Exception e) {
            LOGGER.error(String.format("failed to delete group for groupId=%s by user=%s", groupId, operator), e);
            throw e;
        }

        LOGGER.info("success to delete group for groupId={} by user={}", groupId, operator);
        return true;
    }

    /**
     * Delete InlongGroup logically and delete related resource in a synchronous way.
     */
    public Boolean deleteProcess(String groupId, UserInfo opInfo) {
        InlongGroupEntity entity = groupMapper.selectByGroupId(groupId);
        Preconditions.expectNotNull(entity, ErrorCodeEnum.GROUP_NOT_FOUND, ErrorCodeEnum.GROUP_NOT_FOUND.getMessage());
        // only the person in charges can delete
        if (!opInfo.getAccountType().equals(TenantUserTypeEnum.TENANT_ADMIN.getCode())) {
            List<String> inCharges = Arrays.asList(entity.getInCharges().split(InlongConstants.COMMA));
            if (!inCharges.contains(opInfo.getName())) {
                throw new BusinessException(ErrorCodeEnum.GROUP_PERMISSION_DENIED);
            }
        }
        // check can be deleted
        InlongGroupInfo groupInfo = groupService.doDeleteCheck(groupId, opInfo.getName());
        // start to delete group process
        GroupResourceProcessForm form = genGroupResourceProcessForm(groupInfo, GroupOperateType.DELETE);
        WorkflowResult result = workflowService.start(ProcessName.DELETE_GROUP_PROCESS, opInfo.getName(), form);
        List<TaskResponse> tasks = result.getNewTasks();
        if (TaskStatus.FAILED == tasks.get(tasks.size() - 1).getStatus()) {
            throw new BusinessException(ErrorCodeEnum.WORKFLOW_DELETE_RECORD_FAILED,
                    String.format("failed to delete inlong group for groupId=%s", groupId));
        }
        return true;
    }

    /**
     * Reset InlongGroup status when group is staying CONFIG_ING|SUSPENDING|RESTARTING|DELETING for a long time.
     * This api is side effect, must be used carefully.
     *
     * @param request reset inlong group request
     * @param operator name of operator
     * @return success or false
     */
    public boolean resetGroupStatus(InlongGroupResetRequest request, String operator) {
        LOGGER.info("begin to reset group status by operator={} for request={}", operator, request);
        final String groupId = request.getInlongGroupId();
        InlongGroupInfo groupInfo = groupService.get(groupId);
        Preconditions.expectNotNull(groupInfo, ErrorCodeEnum.GROUP_NOT_FOUND.getMessage());

        GroupStatus status = GroupStatus.forCode(groupInfo.getStatus());
        boolean result;
        switch (status) {
            case CONFIG_ING:
            case CONFIG_OFFLINE_ING:
            case CONFIG_ONLINE_ING:
            case CONFIG_DELETING:
                final int rerunProcess = request.getRerunProcess();
                final int resetFinalStatus = request.getResetFinalStatus();
                result = pendingGroupOpt(groupInfo, operator, status, rerunProcess, resetFinalStatus);
                break;
            default:
                throw new IllegalStateException(String.format("Unsupported status to reset groupId=%s and status=%s",
                        request.getInlongGroupId(), status));
        }

        LOGGER.info("finish to reset group status by operator={}, result={} for request={}", operator, result, request);
        return result;
    }

    private boolean pendingGroupOpt(InlongGroupInfo groupInfo, String operator, GroupStatus status,
            int rerunProcess, int resetFinalStatus) {
        final String groupId = groupInfo.getInlongGroupId();
        if (rerunProcess == 1) {
            ProcessRequest processQuery = new ProcessRequest();
            processQuery.setInlongGroupId(groupId);
            List<WorkflowProcessEntity> entities = workflowQueryService.listProcessEntity(processQuery);
            entities.sort(Comparator.comparingInt(WorkflowProcessEntity::getId));
            WorkflowProcessEntity lastProcess = entities.get(entities.size() - 1);
            UserInfo userInfo = LoginUserUtils.getLoginUser();
            EXECUTOR_SERVICE.execute(
                    () -> workflowService.continueProcessAsync(lastProcess.getId(), userInfo, "Reset group status"));
            return true;
        }
        if (resetFinalStatus == 1) {
            GroupStatus finalStatus = getFinalStatus(status);
            return groupService.updateStatus(groupId, finalStatus.getCode(), operator);
        } else {
            return groupService.updateStatus(groupId, GroupStatus.CONFIG_FAILED.getCode(), operator);
        }
    }

    private GroupStatus getFinalStatus(GroupStatus pendingStatus) {
        switch (pendingStatus) {
            case CONFIG_ING:
            case CONFIG_ONLINE_ING:
                return GroupStatus.CONFIG_SUCCESSFUL;
            case CONFIG_OFFLINE_ING:
                return GroupStatus.CONFIG_OFFLINE_SUCCESSFUL;
            default:
                return GroupStatus.CONFIG_DELETED;
        }
    }

    private void invokeDeleteProcess(String groupId, String operator) {
        // check can be deleted
        InlongGroupInfo groupInfo = groupService.doDeleteCheck(groupId, operator);
        // start to delete group process
        GroupResourceProcessForm form = genGroupResourceProcessForm(groupInfo, GroupOperateType.DELETE);
        WorkflowResult result = workflowService.start(ProcessName.DELETE_GROUP_PROCESS, operator, form);
        List<TaskResponse> tasks = result.getNewTasks();
        if (TaskStatus.FAILED == tasks.get(tasks.size() - 1).getStatus()) {
            String errMsg = String.format("failed to delete inlong group for groupId=%s", groupId);
            LOGGER.error(errMsg);
            throw new WorkflowListenerException(errMsg);
        }
    }

    /**
     * Generate the form of [Apply Group Workflow]
     */
    private ApplyGroupProcessForm genApplyGroupProcessForm(String groupId) {
        ApplyGroupProcessForm form = new ApplyGroupProcessForm();
        InlongGroupInfo groupInfo = groupService.get(groupId);
        form.setGroupInfo(groupInfo);
        List<InlongStreamBriefInfo> infoList = streamService.listBriefWithSink(groupInfo.getInlongGroupId());
        form.setStreamInfoList(infoList);
        return form;
    }

    private ApplyGroupProcessForm genApplyGroupProcessForm(List<String> groupIdList) {
        ApplyGroupProcessForm form = new ApplyGroupProcessForm();
        List<GroupFullInfo> groupFullInfoList = new ArrayList<>();
        for (String groupId : groupIdList) {
            InlongGroupInfo groupInfo = groupService.get(groupId);
            List<InlongStreamBriefInfo> infoList = streamService.listBriefWithSink(groupInfo.getInlongGroupId());
            GroupFullInfo groupFullInfo = new GroupFullInfo();
            groupFullInfo.setGroupInfo(groupInfo);
            groupFullInfo.setStreamInfoList(infoList);
            groupFullInfoList.add(groupFullInfo);
        }
        form.setGroupFullInfoList(groupFullInfoList);
        return form;
    }

    /**
     * Generate the form of [Group Resource Workflow]
     */
    private GroupResourceProcessForm genGroupResourceProcessForm(InlongGroupInfo groupInfo,
            GroupOperateType operateType) {
        GroupResourceProcessForm form = new GroupResourceProcessForm();
        String groupId = groupInfo.getInlongGroupId();
        List<InlongStreamInfo> streamList = streamService.list(groupId);
        form.setStreamInfos(streamList);
        form.setGroupInfo(groupInfo);
        form.setGroupOperateType(operateType);
        return form;
    }

}
