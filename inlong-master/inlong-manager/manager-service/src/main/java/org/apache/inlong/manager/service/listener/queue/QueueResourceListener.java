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

package org.apache.inlong.manager.service.listener.queue;

import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.enums.GroupOperateType;
import org.apache.inlong.manager.common.enums.GroupStatus;
import org.apache.inlong.manager.common.enums.TaskEvent;
import org.apache.inlong.manager.common.enums.TaskStatus;
import org.apache.inlong.manager.common.exceptions.WorkflowListenerException;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.pojo.user.LoginUserUtils;
import org.apache.inlong.manager.pojo.user.UserInfo;
import org.apache.inlong.manager.pojo.workflow.TaskResponse;
import org.apache.inlong.manager.pojo.workflow.WorkflowResult;
import org.apache.inlong.manager.pojo.workflow.form.process.GroupResourceProcessForm;
import org.apache.inlong.manager.pojo.workflow.form.process.StreamResourceProcessForm;
import org.apache.inlong.manager.service.group.InlongGroupService;
import org.apache.inlong.manager.service.resource.queue.QueueResourceOperator;
import org.apache.inlong.manager.service.resource.queue.QueueResourceOperatorFactory;
import org.apache.inlong.manager.service.stream.InlongStreamService;
import org.apache.inlong.manager.service.workflow.WorkflowService;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.event.ListenerResult;
import org.apache.inlong.manager.workflow.event.task.QueueOperateListener;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;

import static org.apache.inlong.manager.common.consts.InlongConstants.ALIVE_TIME_MS;
import static org.apache.inlong.manager.common.consts.InlongConstants.CORE_POOL_SIZE;
import static org.apache.inlong.manager.common.consts.InlongConstants.MAX_POOL_SIZE;
import static org.apache.inlong.manager.common.consts.InlongConstants.QUEUE_SIZE;
import static org.apache.inlong.manager.common.enums.GroupOperateType.INIT;
import static org.apache.inlong.manager.common.enums.ProcessName.CREATE_STREAM_RESOURCE;

/**
 * Create message queue resources,
 * such as Pulsar Topic and Subscription, TubeMQ Topic and ConsumerGroup, etc.
 */
@Slf4j
@Service
public class QueueResourceListener implements QueueOperateListener {

    private static final Integer TIMEOUT_SECONDS = 180;

    private static final ExecutorService EXECUTOR_SERVICE = new ThreadPoolExecutor(
            CORE_POOL_SIZE,
            MAX_POOL_SIZE,
            ALIVE_TIME_MS,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(QUEUE_SIZE),
            new ThreadFactoryBuilder().setNameFormat("inlong-mq-process-%s").build(),
            new CallerRunsPolicy());

    @Autowired
    private InlongGroupService groupService;
    @Autowired
    private InlongStreamService streamService;
    @Autowired
    private WorkflowService workflowService;
    @Autowired
    private QueueResourceOperatorFactory queueOperatorFactory;

    @Override
    public TaskEvent event() {
        return TaskEvent.COMPLETE;
    }

    @Override
    public boolean accept(WorkflowContext context) {
        return isGroupProcessForm(context);
    }

    @Override
    public ListenerResult listen(WorkflowContext context) throws WorkflowListenerException {
        GroupResourceProcessForm groupProcessForm = (GroupResourceProcessForm) context.getProcessForm();
        final String groupId = groupProcessForm.getInlongGroupId();
        // ensure the inlong group exists
        InlongGroupInfo groupInfo = groupService.get(groupId);
        if (groupInfo == null) {
            String msg = "inlong group not found with groupId=" + groupId;
            log.error(msg);
            throw new WorkflowListenerException(msg);
        }
        // Read the current information
        groupProcessForm.setGroupInfo(groupInfo);
        groupProcessForm.setStreamInfos(streamService.list(groupId));

        String operator = context.getOperator();
        GroupOperateType operateType = groupProcessForm.getGroupOperateType();
        if (InlongConstants.DATASYNC_REALTIME_MODE.equals(groupInfo.getInlongGroupMode())
                || InlongConstants.DATASYNC_OFFLINE_MODE.equals(groupInfo.getInlongGroupMode())) {
            log.warn("skip to execute QueueResourceListener as sync mode {} (1 for realtime sync, 2 for offline sync) "
                    + "for groupId={}", groupInfo.getInlongGroupMode(), groupId);
            if (GroupOperateType.INIT.equals(operateType)) {
                this.createQueueForStreams(groupInfo, groupProcessForm.getStreamInfos(), operator);
            }
            return ListenerResult.success("skip - disable create mq resource for sync mode");
        }

        if (InlongConstants.DISABLE_CREATE_RESOURCE.equals(groupInfo.getEnableCreateResource())) {
            log.warn("skip to execute QueueResourceListener as disable create resource for groupId={}", groupId);
            return ListenerResult.success("skip - disable create resource");
        }

        QueueResourceOperator queueOperator = queueOperatorFactory.getInstance(groupInfo.getMqType());
        switch (operateType) {
            case INIT:
                groupService.updateStatus(groupId, GroupStatus.CONFIG_ING.getCode(), operator);
                // create queue resource for inlong group
                queueOperator.createQueueForGroup(groupInfo, operator);
                // create queue resource for all inlong streams under the inlong group
                this.createQueueForStreams(groupInfo, groupProcessForm.getStreamInfos(), operator);
                break;
            case DELETE:
                groupService.updateStatus(groupId, GroupStatus.CONFIG_DELETING.getCode(), operator);
                queueOperator.deleteQueueForGroup(groupInfo, operator);
                break;
            default:
                log.warn("unsupported operate={} for inlong group", operateType);
                break;
        }

        log.info("success to execute QueueResourceListener for groupId={}, operateType={}", groupId, operateType);
        return ListenerResult.success("success");
    }

    private void createQueueForStreams(InlongGroupInfo groupInfo, List<InlongStreamInfo> streamInfos, String operator) {
        String groupId = groupInfo.getInlongGroupId();
        log.info("success to start stream process for groupId={}", groupId);

        for (InlongStreamInfo stream : streamInfos) {
            StreamResourceProcessForm form = StreamResourceProcessForm.getProcessForm(groupInfo, stream, INIT);
            String streamId = stream.getInlongStreamId();
            final String errMsg = "failed to start stream process for groupId=" + groupId + " streamId=" + streamId;
            UserInfo userInfo = LoginUserUtils.getLoginUser();
            CompletableFuture<WorkflowResult> future = CompletableFuture
                    .supplyAsync(() -> workflowService.startAsync(CREATE_STREAM_RESOURCE, userInfo, form),
                            EXECUTOR_SERVICE)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error(errMsg + ": " + ex.getMessage());
                            throw new WorkflowListenerException(errMsg, ex);
                        } else {
                            List<TaskResponse> tasks = result.getNewTasks();
                            if (TaskStatus.FAILED == tasks.get(tasks.size() - 1).getStatus()) {
                                log.error(errMsg);
                                throw new WorkflowListenerException(errMsg);
                            }
                        }
                    });
            try {
                // wait for the current process complete before starting the next stream,
                // otherwise, an exception is thrown and the next stream process will not be started.
                future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (Exception e) {
                String msg = "failed to execute stream process in asynchronously ";
                log.error(msg, e);
                throw new WorkflowListenerException(msg + ": " + e.getMessage());
            }
        }

        log.info("success to start stream process for groupId={}", groupId);
    }

}
