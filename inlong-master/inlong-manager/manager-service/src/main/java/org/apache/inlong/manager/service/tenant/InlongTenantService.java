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

package org.apache.inlong.manager.service.tenant;

import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.tenant.InlongTenantInfo;
import org.apache.inlong.manager.pojo.tenant.InlongTenantPageRequest;
import org.apache.inlong.manager.pojo.tenant.InlongTenantRequest;
import org.apache.inlong.manager.pojo.user.UserInfo;

/**
 * Inlong tenant service
 */
public interface InlongTenantService {

    /**
     * Get tenant info by tenant name.
     *
     * @param name tenant name
     * @return tenant info
     */
    InlongTenantInfo getByName(String name);

    /**
     * Save inlong tenant info
     *
     * @param request tenant info
     * @return tenant id after saving
     */
    Integer save(InlongTenantRequest request);

    /**
     * Paging query stream sink info based on conditions.
     *
     * @param request paging request
     * @param userInfo query user info
     * @return tenant page list
     */
    PageResult<InlongTenantInfo> listByCondition(InlongTenantPageRequest request, UserInfo userInfo);

    /**
     * Update one tenant
     *
     * @param request tenant request that needs to be modified
     * @return whether succeed
     */
    Boolean update(InlongTenantRequest request);

    /**
     * Delete tenant by name
     *
     * @param name tenant name
     * @return true= delete success/ false = delete fail
     */
    Boolean delete(String name);

    Boolean migrate(String groupId, String to);
}
