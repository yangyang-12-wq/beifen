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

package org.apache.inlong.manager.web.controller;

import org.apache.inlong.manager.common.enums.OperationTarget;
import org.apache.inlong.manager.common.enums.OperationType;
import org.apache.inlong.manager.common.enums.TenantUserTypeEnum;
import org.apache.inlong.manager.common.validation.SaveValidation;
import org.apache.inlong.manager.common.validation.UpdateValidation;
import org.apache.inlong.manager.pojo.common.BatchResult;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.common.Response;
import org.apache.inlong.manager.pojo.group.InlongGroupBriefInfo;
import org.apache.inlong.manager.pojo.group.InlongGroupCountResponse;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.group.InlongGroupPageRequest;
import org.apache.inlong.manager.pojo.group.InlongGroupRequest;
import org.apache.inlong.manager.pojo.group.InlongGroupResetRequest;
import org.apache.inlong.manager.pojo.group.InlongGroupTopicInfo;
import org.apache.inlong.manager.pojo.group.InlongGroupTopicRequest;
import org.apache.inlong.manager.pojo.schedule.OfflineJobRequest;
import org.apache.inlong.manager.pojo.user.LoginUserUtils;
import org.apache.inlong.manager.pojo.workflow.WorkflowResult;
import org.apache.inlong.manager.service.group.InlongGroupProcessService;
import org.apache.inlong.manager.service.group.InlongGroupService;
import org.apache.inlong.manager.service.operationlog.OperationLog;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Inlong group control layer
 */
@RestController
@RequestMapping("/api")
@Api(tags = "Inlong-Group-API")
public class InlongGroupController {

    @Autowired
    private InlongGroupService groupService;
    @Autowired
    private InlongGroupProcessService groupProcessOperation;

    @RequestMapping(value = "/group/save", method = RequestMethod.POST)
    @OperationLog(operation = OperationType.CREATE, operationTarget = OperationTarget.GROUP)
    @ApiOperation(value = "Save inlong group")
    public Response<String> save(@Validated(SaveValidation.class) @RequestBody InlongGroupRequest groupRequest) {
        String operator = LoginUserUtils.getLoginUser().getName();
        return Response.success(groupService.save(groupRequest, operator));
    }

    @RequestMapping(value = "/group/batchSave", method = RequestMethod.POST)
    @OperationLog(operation = OperationType.CREATE, operationTarget = OperationTarget.GROUP)
    @ApiOperation(value = "Batch Save inlong group")
    public Response<List<BatchResult>> batchSave(
            @Validated(SaveValidation.class) @RequestBody List<InlongGroupRequest> groupRequestList) {
        String operator = LoginUserUtils.getLoginUser().getName();
        return Response.success(groupService.batchSave(groupRequestList, operator));
    }

    @RequestMapping(value = "/group/exist/{groupId}", method = RequestMethod.GET)
    @ApiOperation(value = "Is the inlong group id exists")
    @ApiImplicitParam(name = "groupId", value = "Inlong group id", dataTypeClass = String.class, required = true)
    public Response<Boolean> exist(@PathVariable String groupId) {
        return Response.success(groupService.exist(groupId));
    }

    @RequestMapping(value = "/group/get/{groupId}", method = RequestMethod.GET)
    @ApiOperation(value = "Get inlong group")
    @ApiImplicitParam(name = "groupId", value = "Inlong group id", dataTypeClass = String.class, required = true)
    public Response<InlongGroupInfo> get(@PathVariable String groupId) {
        return Response.success(groupService.get(groupId));
    }

    @RequestMapping(value = "/group/getTenant/{groupId}", method = RequestMethod.GET)
    @ApiOperation(value = "Get inlong group tenant")
    @ApiImplicitParam(name = "groupId", value = "Inlong group id", dataTypeClass = String.class, required = true)
    public Response<String> getTenant(@PathVariable String groupId) {
        return Response.success(groupService.getTenant(groupId, LoginUserUtils.getLoginUser().getName()));
    }

    @RequestMapping(value = "/group/countByStatus", method = RequestMethod.GET)
    @ApiOperation(value = "Count inlong group status for current user")
    @ApiImplicitParam(name = "inlongGroupMode", dataTypeClass = Integer.class, defaultValue = "0")
    public Response<InlongGroupCountResponse> countGroupByUser(
            @RequestParam(required = false, defaultValue = "0") Integer inlongGroupMode) {
        String operator = LoginUserUtils.getLoginUser().getName();
        return Response.success(groupService.countGroupByUser(operator, inlongGroupMode));
    }

    @GetMapping(value = "/group/getTopic/{groupId}")
    @ApiOperation(value = "Get topic info")
    public Response<InlongGroupTopicInfo> getTopic(@PathVariable String groupId) {
        return Response.success(groupService.getTopic(groupId));
    }

    @PostMapping(value = "/group/listTopics")
    @ApiOperation(value = "Get topic infos")
    public Response<List<InlongGroupTopicInfo>> listTopics(@RequestBody InlongGroupTopicRequest request) {
        return Response.success(groupService.listTopics(request));
    }

    @GetMapping(value = "/group/getBackupTopic/{groupId}")
    @ApiOperation(value = "Get backup topic info")
    public Response<InlongGroupTopicInfo> getBackupTopic(@PathVariable String groupId) {
        return Response.success(groupService.getBackupTopic(groupId));
    }

    @RequestMapping(value = "/group/list", method = RequestMethod.POST)
    @ApiOperation(value = "List inlong groups by paginating")
    public Response<PageResult<InlongGroupBriefInfo>> listBrief(@RequestBody InlongGroupPageRequest request) {
        request.setCurrentUser(LoginUserUtils.getLoginUser().getName());
        request.setIsAdminRole(
                LoginUserUtils.getLoginUser().getRoles().contains(TenantUserTypeEnum.TENANT_ADMIN.name()));
        return Response.success(groupService.listBrief(request));
    }

    @RequestMapping(value = "/group/update", method = RequestMethod.POST)
    @OperationLog(operation = OperationType.UPDATE, operationTarget = OperationTarget.GROUP)
    @ApiOperation(value = "Update inlong group")
    public Response<String> update(@Validated(UpdateValidation.class) @RequestBody InlongGroupRequest groupRequest) {
        String operator = LoginUserUtils.getLoginUser().getName();
        return Response.success(groupService.update(groupRequest, operator));
    }

    @RequestMapping(value = "/group/delete/{groupId}", method = RequestMethod.DELETE)
    @ApiOperation(value = "Delete inlong group info")
    @OperationLog(operation = OperationType.DELETE, operationTarget = OperationTarget.GROUP)
    @ApiImplicitParam(name = "groupId", value = "Inlong group id", dataTypeClass = String.class, required = true)
    public Response<Boolean> delete(@PathVariable String groupId) {
        String operator = LoginUserUtils.getLoginUser().getName();
        return Response.success(groupProcessOperation.deleteProcess(groupId, operator));
    }

    @RequestMapping(value = "/group/deleteAsync/{groupId}", method = RequestMethod.DELETE)
    @ApiOperation(value = "Delete inlong group info")
    @OperationLog(operation = OperationType.DELETE, operationTarget = OperationTarget.GROUP)
    @ApiImplicitParam(name = "groupId", value = "Inlong group id", dataTypeClass = String.class, required = true)
    public Response<String> deleteAsync(@PathVariable String groupId) {
        String operator = LoginUserUtils.getLoginUser().getName();
        return Response.success(groupProcessOperation.deleteProcessAsync(groupId, operator));
    }

    @RequestMapping(value = "/group/startProcess/{groupId}", method = RequestMethod.POST)
    @ApiOperation(value = "Start inlong approval process")
    @OperationLog(operation = OperationType.START, operationTarget = OperationTarget.GROUP)
    @ApiImplicitParam(name = "groupId", value = "Inlong group id", dataTypeClass = String.class)
    public Response<WorkflowResult> startProcess(@PathVariable String groupId) {
        String operator = LoginUserUtils.getLoginUser().getName();
        return Response.success(groupProcessOperation.startProcess(groupId, operator));
    }

    @RequestMapping(value = "/group/batchStartProcess", method = RequestMethod.POST)
    @ApiOperation(value = "Batch start inlong approval process")
    @OperationLog(operation = OperationType.START, operationTarget = OperationTarget.GROUP)
    public Response<WorkflowResult> batchStartProcess(@RequestBody List<String> groupIdList) {
        String operator = LoginUserUtils.getLoginUser().getName();
        return Response.success(groupProcessOperation.batchStartProcess(groupIdList, operator));
    }

    @RequestMapping(value = "/group/suspendProcess/{groupId}", method = RequestMethod.POST)
    @ApiOperation(value = "Suspend inlong group process")
    @OperationLog(operation = OperationType.SUSPEND, operationTarget = OperationTarget.GROUP)
    @ApiImplicitParam(name = "groupId", value = "Inlong group id", dataTypeClass = String.class)
    public Response<WorkflowResult> suspendProcess(@PathVariable String groupId) {
        String operator = LoginUserUtils.getLoginUser().getName();
        return Response.success(groupProcessOperation.suspendProcess(groupId, operator));
    }

    @RequestMapping(value = "/group/restartProcess/{groupId}", method = RequestMethod.POST)
    @ApiOperation(value = "Restart inlong group process")
    @ApiImplicitParam(name = "groupId", value = "Inlong group id", dataTypeClass = String.class)
    public Response<WorkflowResult> restartProcess(@PathVariable String groupId) {
        String operator = LoginUserUtils.getLoginUser().getName();
        return Response.success(groupProcessOperation.restartProcess(groupId, operator));
    }

    @RequestMapping(value = "/group/suspendProcessAsync/{groupId}", method = RequestMethod.POST)
    @OperationLog(operation = OperationType.SUSPEND, operationTarget = OperationTarget.GROUP)
    @ApiOperation(value = "Suspend inlong group process")
    @ApiImplicitParam(name = "groupId", value = "Inlong group id", dataTypeClass = String.class)
    public Response<String> suspendProcessAsync(@PathVariable String groupId) {
        String operator = LoginUserUtils.getLoginUser().getName();
        return Response.success(groupProcessOperation.suspendProcessAsync(groupId, operator));
    }

    @RequestMapping(value = "/group/restartProcessAsync/{groupId}", method = RequestMethod.POST)
    @ApiOperation(value = "Restart inlong group process")
    @ApiImplicitParam(name = "groupId", value = "Inlong group id", dataTypeClass = String.class)
    public Response<String> restartProcessAsync(@PathVariable String groupId) {
        String operator = LoginUserUtils.getLoginUser().getName();
        return Response.success(groupProcessOperation.restartProcessAsync(groupId, operator));
    }

    @PostMapping(value = "/group/reset")
    @ApiOperation(value = "Reset group status when group is in CONFIG_ING|SUSPENDING|RESTARTING|DELETING")
    public Response<Boolean> reset(@RequestBody @Validated InlongGroupResetRequest request) {
        String operator = LoginUserUtils.getLoginUser().getName();
        return Response.success(groupProcessOperation.resetGroupStatus(request, operator));
    }

    @RequestMapping(value = "/group/detail/{groupId}", method = RequestMethod.GET)
    @ApiOperation(value = "get group detail")
    @ApiImplicitParam(name = "groupId", value = "Inlong group id", dataTypeClass = String.class, required = true)
    public Response<Map<String, Object>> detail(@PathVariable String groupId) {
        return Response.success(groupService.detail(groupId));
    }

    @RequestMapping(value = "/group/switch/start/{groupId}/{clusterTag}", method = RequestMethod.GET)
    @ApiOperation(value = "start tag switch")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "groupId", value = "Inlong group id", dataTypeClass = String.class, required = true),
            @ApiImplicitParam(name = "clusterTag", value = "cluster tag", dataTypeClass = String.class, required = true)
    })
    public Response<Boolean> startTagSwitch(@PathVariable String groupId, @PathVariable String clusterTag) {
        return Response.success(groupService.startTagSwitch(groupId, clusterTag));
    }

    @RequestMapping(value = "/group/switch/finish/{groupId}", method = RequestMethod.GET)
    @ApiOperation(value = "finish tag switch")
    @ApiImplicitParam(name = "groupId", value = "Inlong group id", dataTypeClass = String.class, required = true)
    public Response<Boolean> finishTagSwitch(@PathVariable String groupId) {
        return Response.success(groupService.finishTagSwitch(groupId));
    }

    @RequestMapping(value = "/group/submitOfflineJob", method = RequestMethod.POST)
    @ApiOperation(value = "Submitting inlong offline job process")
    public Response<Boolean> submitOfflineJob(@RequestBody OfflineJobRequest request) {
        return Response.success(groupService.submitOfflineJob(request));
    }
}