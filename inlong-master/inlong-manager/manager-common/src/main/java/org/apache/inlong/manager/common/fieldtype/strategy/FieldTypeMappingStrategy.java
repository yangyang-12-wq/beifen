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

/**
 * The interface of base field type mapping strategy operation.
 */
public interface FieldTypeMappingStrategy {

    Boolean accept(String type);

    /**
     * Get the field type of inlong field type mapping by the source field type.
     *
     * @param sourceType the source field type
     * @return the target field type of inlong field type mapping
     */
    String getSourceToSinkFieldTypeMapping(String sourceType);

    /**
     * Get the field type of inlong field type mapping by the stream field type.
     *
     * @param streamType the stream field type
     * @return the target field type of inlong field type mapping
     */
    String getStreamToSinkFieldTypeMapping(String streamType);

}
