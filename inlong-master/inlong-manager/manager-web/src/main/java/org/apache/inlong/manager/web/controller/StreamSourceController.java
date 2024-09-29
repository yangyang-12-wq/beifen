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
import org.apache.inlong.manager.common.validation.SaveValidation;
import org.apache.inlong.manager.common.validation.UpdateValidation;
import org.apache.inlong.manager.pojo.common.BatchResult;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.common.Response;
import org.apache.inlong.manager.pojo.source.DataAddTaskRequest;
import org.apache.inlong.manager.pojo.source.SourcePageRequest;
import org.apache.inlong.manager.pojo.source.SourceRequest;
import org.apache.inlong.manager.pojo.source.StreamSource;
import org.apache.inlong.manager.pojo.user.LoginUserUtils;
import org.apache.inlong.manager.service.operationlog.OperationLog;
import org.apache.inlong.manager.service.source.StreamSourceService;

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
 * Stream source control layer
 */
@RestController
@RequestMapping("/api")
@Api(tags = "Stream-Source-API")
public class StreamSourceController {

    @Autowired
    StreamSourceService sourceService;

    @RequestMapping(value = "/source/save", method = RequestMethod.POST)
    @OperationLog(operation = OperationType.CREATE, operationTarget = OperationTarget.SOURCE)
    @ApiOperation(value = "Save stream source")
    public Response<Integer> save(@Validated(SaveValidation.class) @RequestBody SourceRequest request) {
        return Response.success(sourceService.save(request, LoginUserUtils.getLoginUser().getName()));
    }

    @RequestMapping(value = "/source/batchSave", method = RequestMethod.POST)
    @OperationLog(operation = OperationType.CREATE, operationTarget = OperationTarget.SOURCE)
    @ApiOperation(value = "Batch save stream source")
    public Response<List<BatchResult>> batchSave(
            @Validated(SaveValidation.class) @RequestBody List<SourceRequest> requestList) {
        return Response.success(sourceService.batchSave(requestList, LoginUserUtils.getLoginUser().getName()));
    }

    @RequestMapping(value = "/source/get/{id}", method = RequestMethod.GET)
    @ApiOperation(value = "Get stream source")
    @ApiImplicitParam(name = "id", dataTypeClass = Integer.class, required = true)
    public Response<StreamSource> get(@PathVariable Integer id) {
        return Response.success(sourceService.get(id));
    }

    @RequestMapping(value = "/source/list", method = RequestMethod.POST)
    @ApiOperation(value = "List stream sources by paginating")
    public Response<PageResult<? extends StreamSource>> listByCondition(@RequestBody SourcePageRequest request) {
        return Response.success(sourceService.listByCondition(request));
    }

    @RequestMapping(value = "/source/update", method = RequestMethod.POST)
    @OperationLog(operation = OperationType.UPDATE, operationTarget = OperationTarget.SOURCE)
    @ApiOperation(value = "Update stream source")
    public Response<Boolean> update(@Validated(UpdateValidation.class) @RequestBody SourceRequest request) {
        return Response.success(sourceService.update(request, LoginUserUtils.getLoginUser().getName()));
    }

    @RequestMapping(value = "/source/delete/{id}", method = RequestMethod.DELETE)
    @OperationLog(operation = OperationType.DELETE, operationTarget = OperationTarget.SOURCE)
    @ApiOperation(value = "Delete stream source")
    @ApiImplicitParam(name = "id", dataTypeClass = Integer.class, required = true)
    public Response<Boolean> delete(@PathVariable Integer id) {
        boolean result = sourceService.delete(id, LoginUserUtils.getLoginUser().getName());
        return Response.success(result);
    }

    @RequestMapping(value = "/source/stop/{id}", method = RequestMethod.POST)
    @ApiOperation(value = "stop stream source")
    @ApiImplicitParam(name = "id", dataTypeClass = Integer.class, required = true)
    public Response<Boolean> stop(@PathVariable Integer id) {
        boolean result = sourceService.stop(id, LoginUserUtils.getLoginUser().getName());
        return Response.success(result);
    }

    @RequestMapping(value = "/source/restart/{id}", method = RequestMethod.POST)
    @ApiOperation(value = "restart stream source")
    @ApiImplicitParam(name = "id", dataTypeClass = Integer.class, required = true)
    public Response<Boolean> restart(@PathVariable Integer id) {
        boolean result = sourceService.restart(id, LoginUserUtils.getLoginUser().getName());
        return Response.success(result);
    }

    @RequestMapping(value = "/source/forceDelete", method = RequestMethod.DELETE)
    @OperationLog(operation = OperationType.DELETE, operationTarget = OperationTarget.SOURCE)
    @ApiOperation(value = "Force delete stream source by groupId and streamId")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "inlongGroupId", dataTypeClass = String.class, required = true),
            @ApiImplicitParam(name = "inlongStreamId", dataTypeClass = String.class, required = true)
    })
    public Response<Boolean> forceDelete(@RequestParam String inlongGroupId, @RequestParam String inlongStreamId) {
        return Response.success(
                sourceService.forceDelete(inlongGroupId, inlongStreamId, LoginUserUtils.getLoginUser().getName()));
    }

    @RequestMapping(value = "/source/addDataAddTask/{groupId}", method = RequestMethod.POST)
    @ApiOperation(value = "Add supplementary recording task for stream source")
    @ApiImplicitParam(name = "groupId", dataTypeClass = String.class, required = true)
    public Response<List<Integer>> addSub(@PathVariable String groupId,
            @RequestBody List<DataAddTaskRequest> requestList) {
        return Response.success(
                sourceService.batchAddDataAddTask(groupId, requestList, LoginUserUtils.getLoginUser().getName()));
    }

}
