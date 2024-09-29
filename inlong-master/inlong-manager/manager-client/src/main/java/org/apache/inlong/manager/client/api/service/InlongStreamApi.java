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

package org.apache.inlong.manager.client.api.service;

import org.apache.inlong.manager.pojo.common.BatchResult;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.common.Response;
import org.apache.inlong.manager.pojo.consume.BriefMQMessage;
import org.apache.inlong.manager.pojo.sink.ParseFieldRequest;
import org.apache.inlong.manager.pojo.stream.InlongStreamBriefInfo;
import org.apache.inlong.manager.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.pojo.stream.InlongStreamPageRequest;
import org.apache.inlong.manager.pojo.stream.StreamField;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

import java.util.List;
import java.util.Map;

public interface InlongStreamApi {

    @POST("stream/save")
    Call<Response<Integer>> createStream(@Body InlongStreamInfo stream);

    @POST("stream/batchSave")
    Call<Response<List<BatchResult>>> batchCreateStream(@Body List<InlongStreamInfo> streamInfos);

    @GET("stream/exist/{groupId}/{streamId}")
    Call<Response<Boolean>> isStreamExists(@Path("groupId") String groupId, @Path("streamId") String streamId);

    @POST("stream/update")
    Call<Response<Boolean>> updateStream(@Body InlongStreamInfo stream);

    @GET("stream/get")
    Call<Response<InlongStreamInfo>> getStream(@Query("groupId") String groupId,
            @Query("streamId") String streamId);

    @GET("stream/getBrief")
    Call<Response<InlongStreamBriefInfo>> getStreamBriefInfo(@Query("groupId") String groupId,
            @Query("streamId") String streamId);

    @POST("stream/list")
    Call<Response<PageResult<InlongStreamBriefInfo>>> listByCondition(@Body InlongStreamPageRequest request);

    @POST("stream/listAll")
    Call<Response<PageResult<InlongStreamInfo>>> listStream(@Body InlongStreamPageRequest request);

    @POST("stream/startProcess/{groupId}/{streamId}")
    Call<Response<Boolean>> startProcess(@Path("groupId") String groupId, @Path("streamId") String streamId,
            @Query("sync") boolean sync);

    @POST("stream/suspendProcess/{groupId}/{streamId}")
    Call<Response<Boolean>> suspendProcess(@Path("groupId") String groupId, @Path("streamId") String streamId,
            @Query("sync") boolean sync);

    @POST("stream/restartProcess/{groupId}/{streamId}")
    Call<Response<Boolean>> restartProcess(@Path("groupId") String groupId, @Path("streamId") String streamId,
            @Query("sync") boolean sync);

    @POST("stream/deleteProcess/{groupId}/{streamId}")
    Call<Response<Boolean>> deleteProcess(@Path("groupId") String groupId, @Path("streamId") String streamId,
            @Query("sync") boolean sync);

    @DELETE("stream/delete")
    Call<Response<Boolean>> delete(@Query("groupId") String groupId, @Query("streamId") String streamId);

    @POST("stream/parseFields")
    Call<Response<List<StreamField>>> parseFields(@Body ParseFieldRequest parseFieldRequest);

    @GET("stream/listMessages")
    Call<Response<List<BriefMQMessage>>> listMessages(@QueryMap Map<String, Object> query);
}
