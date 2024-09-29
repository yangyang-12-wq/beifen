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

package org.apache.inlong.common.pojo.sort.dataflow.field.format;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.DeserializationFeature;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.SerializationFeature;

import javax.annotation.Nonnull;

import java.io.IOException;

/**
 * A utility class to marshall and demarshall format information.
 */
public class FormatUtils {

    /**
     * Returns the json representing the given format.
     *
     * @param format The format to be marshalled.
     * @return The json representing the given format.
     */
    public static String marshall(
            @Nonnull FormatInfo format) {
        ObjectMapper objectMapper = getObjectMapper();
        try {
            return objectMapper.writeValueAsString(format);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize the format " + format + ".", e);
        }
    }

    /**
     * Returns the format represented by the given json.
     *
     * @param json The json representing the given format.
     * @return The format represented by the given json.
     * @throws IOException Thrown when the format cannot be demarshalled.
     */
    public static FormatInfo demarshall(
            @Nonnull String json) throws IOException {
        ObjectMapper objectMapper = getObjectMapper();
        return objectMapper.readValue(json, FormatInfo.class);
    }

    private static ObjectMapper getObjectMapper() {
        return new ObjectMapper()
                .enable(
                        DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES,
                        DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES,
                        DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
                .disable(
                        SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    private FormatUtils() {
    }
}
