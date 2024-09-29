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

package org.apache.inlong.manager.schedule;

import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ScheduleClientFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduleClientFactory.class);

    @Value("${inlong.schedule.engine:none}")
    private String scheduleEngineName;

    @Autowired
    List<ScheduleEngineClient> scheduleEngineClients;

    public ScheduleEngineClient getInstance() {
        Optional<ScheduleEngineClient> optScheduleClient =
                scheduleEngineClients.stream().filter(t -> t.accept(scheduleEngineName)).findFirst();
        if (!optScheduleClient.isPresent()) {
            LOGGER.warn("Schedule engine client not found for {} ", scheduleEngineName);
            throw new BusinessException(ErrorCodeEnum.SCHEDULE_ENGINE_NOT_SUPPORTED,
                    String.format(ErrorCodeEnum.SCHEDULE_ENGINE_NOT_SUPPORTED.getMessage(), scheduleEngineName));
        }
        LOGGER.info("Get schedule engine client success for {}", scheduleEngineName);
        return optScheduleClient.get();
    }

}
