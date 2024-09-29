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

import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.common.Response;
import org.apache.inlong.manager.pojo.tenant.InlongTenantInfo;
import org.apache.inlong.manager.pojo.tenant.InlongTenantPageRequest;
import org.apache.inlong.manager.pojo.tenant.InlongTenantRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface InlongTenantApi {

    @POST("tenant/save")
    Call<Response<Integer>> createInLongTenant(@Body InlongTenantRequest request);

    @POST("tenant/list")
    Call<Response<PageResult<InlongTenantInfo>>> listByCondition(@Body InlongTenantPageRequest request);

    @POST("tenant/update")
    Call<Response<Boolean>> update(@Body InlongTenantRequest request);

    @GET("tenant/get/{name}")
    Call<Response<InlongTenantInfo>> get(@Path("name") String name);

    @DELETE("tenant/delete/{name}")
    Call<Response<Boolean>> delete(@Path("name") String name);

    @GET("tenant/migrate/{group}/{tenant}")
    Call<Response<Boolean>> migrate(@Path("group") String group, @Path("tenant") String tenant);

}
