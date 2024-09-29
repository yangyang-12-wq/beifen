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

package org.apache.inlong.manager.web.controller.openapi;

import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.OperationTarget;
import org.apache.inlong.manager.common.enums.OperationType;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.common.validation.UpdateValidation;
import org.apache.inlong.manager.pojo.common.BatchResult;
import org.apache.inlong.manager.pojo.common.Response;
import org.apache.inlong.manager.pojo.sink.AddFieldRequest;
import org.apache.inlong.manager.pojo.stream.InlongStreamBriefInfo;
import org.apache.inlong.manager.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.pojo.stream.InlongStreamPageRequest;
import org.apache.inlong.manager.pojo.stream.InlongStreamRequest;
import org.apache.inlong.manager.pojo.user.LoginUserUtils;
import org.apache.inlong.manager.service.operationlog.OperationLog;
import org.apache.inlong.manager.service.stream.InlongStreamProcessService;
import org.apache.inlong.manager.service.stream.InlongStreamService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Open InLong Stream controller
 */
@RestController
@RequestMapping("/openapi")
@Api(tags = "Open-InLongStream-API")
public class OpenInLongStreamController {

    @Autowired
    private InlongStreamService streamService;
    @Autowired
    private InlongStreamProcessService streamProcessOperation;

    @RequestMapping(value = "/stream/get", method = RequestMethod.GET)
    @ApiOperation(value = "Get inlong stream")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "groupId", dataTypeClass = String.class, required = true),
            @ApiImplicitParam(name = "streamId", dataTypeClass = String.class, required = true)
    })
    public Response<InlongStreamInfo> get(@RequestParam String groupId, @RequestParam String streamId) {
        Preconditions.expectNotBlank(groupId, ErrorCodeEnum.INVALID_PARAMETER, "groupId cannot be blank");
        Preconditions.expectNotBlank(streamId, ErrorCodeEnum.INVALID_PARAMETER, "streamId cannot be blank");
        Preconditions.expectNotNull(LoginUserUtils.getLoginUser(), ErrorCodeEnum.LOGIN_USER_EMPTY);
        return Response.success(streamService.get(groupId, streamId));
    }

    @RequestMapping(value = "/stream/getBrief", method = RequestMethod.GET)
    @ApiOperation(value = "Get inlong stream brief")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "groupId", dataTypeClass = String.class, required = true),
            @ApiImplicitParam(name = "streamId", dataTypeClass = String.class, required = true)
    })
    public Response<InlongStreamBriefInfo> getBrief(@RequestParam String groupId, @RequestParam String streamId) {
        Preconditions.expectNotBlank(groupId, ErrorCodeEnum.INVALID_PARAMETER, "groupId cannot be blank");
        Preconditions.expectNotBlank(streamId, ErrorCodeEnum.INVALID_PARAMETER, "streamId cannot be blank");
        Preconditions.expectNotNull(LoginUserUtils.getLoginUser(), ErrorCodeEnum.LOGIN_USER_EMPTY);
        String operator = LoginUserUtils.getLoginUser().getName();
        return Response.success(streamService.getBrief(groupId, streamId, operator));
    }

    @RequestMapping(value = "/stream/list", method = RequestMethod.POST)
    @ApiOperation(value = "List inlong stream briefs by paginating")
    public Response<List<InlongStreamBriefInfo>> listByCondition(@RequestBody InlongStreamPageRequest request) {
        Preconditions.expectNotNull(request, ErrorCodeEnum.INVALID_PARAMETER, "request cannot be null");
        Preconditions.expectNotNull(LoginUserUtils.getLoginUser(), ErrorCodeEnum.LOGIN_USER_EMPTY);
        return Response.success(streamService.listBrief(request, LoginUserUtils.getLoginUser()));
    }

    @RequestMapping(value = "/stream/save", method = RequestMethod.POST)
    @OperationLog(operation = OperationType.CREATE, operationTarget = OperationTarget.STREAM)
    @ApiOperation(value = "Save inlong stream")
    public Response<Integer> save(@RequestBody InlongStreamRequest request) {
        Preconditions.expectNotNull(request, ErrorCodeEnum.INVALID_PARAMETER, "request cannot be null");
        Preconditions.expectNotNull(LoginUserUtils.getLoginUser(), ErrorCodeEnum.LOGIN_USER_EMPTY);
        return Response.success(streamService.save(request, LoginUserUtils.getLoginUser().getName()));
    }

    @RequestMapping(value = "/stream/batchSave", method = RequestMethod.POST)
    @OperationLog(operation = OperationType.CREATE, operationTarget = OperationTarget.STREAM)
    @ApiOperation(value = "Batch save inlong stream")
    public Response<List<BatchResult>> batchSave(@RequestBody List<InlongStreamRequest> requestList) {
        List<BatchResult> result = streamService.batchSave(requestList, LoginUserUtils.getLoginUser().getName());
        return Response.success(result);
    }

    @RequestMapping(value = "/stream/update", method = RequestMethod.POST)
    @OperationLog(operation = OperationType.UPDATE, operationTarget = OperationTarget.STREAM)
    @ApiOperation(value = "Update inlong stream")
    public Response<Boolean> update(@Validated(UpdateValidation.class) @RequestBody InlongStreamRequest request) {
        Preconditions.expectNotNull(request, ErrorCodeEnum.INVALID_PARAMETER, "request cannot be null");
        Preconditions.expectNotNull(LoginUserUtils.getLoginUser(), ErrorCodeEnum.LOGIN_USER_EMPTY);
        return Response.success(streamService.update(request, LoginUserUtils.getLoginUser().getName()));
    }

    @RequestMapping(value = "/stream/delete", method = RequestMethod.DELETE)
    @OperationLog(operation = OperationType.DELETE, operationTarget = OperationTarget.STREAM)
    @ApiOperation(value = "Delete inlong stream")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "groupId", dataTypeClass = String.class, required = true),
            @ApiImplicitParam(name = "streamId", dataTypeClass = String.class, required = true)
    })
    public Response<Boolean> delete(@RequestParam String groupId, @RequestParam String streamId) {
        Preconditions.expectNotBlank(groupId, ErrorCodeEnum.INVALID_PARAMETER, "groupId cannot be blank");
        Preconditions.expectNotBlank(streamId, ErrorCodeEnum.INVALID_PARAMETER, "streamId cannot be blank");
        Preconditions.expectNotNull(LoginUserUtils.getLoginUser(), ErrorCodeEnum.LOGIN_USER_EMPTY);
        return Response.success(streamService.delete(groupId, streamId, LoginUserUtils.getLoginUser().getName()));
    }

    @RequestMapping(value = "/stream/startProcess/{groupId}/{streamId}", method = RequestMethod.POST)
    @ApiOperation(value = "Start inlong stream process")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "groupId", dataTypeClass = String.class, required = true),
            @ApiImplicitParam(name = "streamId", dataTypeClass = String.class, required = true)
    })
    public Response<Boolean> startProcess(@PathVariable String groupId, @PathVariable String streamId,
            @RequestParam(required = false, defaultValue = "false") boolean sync) {
        String operator = LoginUserUtils.getLoginUser().getName();
        return Response.success(streamProcessOperation.startProcess(groupId, streamId, operator, sync));
    }

    @RequestMapping(value = "/stream/addFields", method = RequestMethod.POST)
    @ApiOperation(value = "Add inlong stream fields")
    public Response<Boolean> addFields(@RequestBody AddFieldRequest addFieldsRequest) {
        return Response.success(streamService.addFields(addFieldsRequest));
    }
}
