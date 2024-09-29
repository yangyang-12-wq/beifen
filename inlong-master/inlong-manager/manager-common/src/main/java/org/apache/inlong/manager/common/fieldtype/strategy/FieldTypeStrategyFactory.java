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

package org.apache.inlong.manager.common.fieldtype.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Factory for {@link FieldTypeMappingStrategy}.
 */
@Service
@Slf4j
public class FieldTypeStrategyFactory {

    @Autowired
    private List<FieldTypeMappingStrategy> strategyList;

    /**
     * Get a field type mapping strategy instance.
     *
     * @param type type
     */
    public FieldTypeMappingStrategy getInstance(String type) {
        return strategyList.stream()
                .filter(inst -> inst.accept(type))
                .findFirst()
                .orElse(null);
    }

}
