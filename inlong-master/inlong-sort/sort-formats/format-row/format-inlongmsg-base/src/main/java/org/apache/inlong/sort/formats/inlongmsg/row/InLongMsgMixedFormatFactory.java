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

package org.apache.inlong.sort.formats.inlongmsg.row;

import java.util.Map;

/**
 * Factory for creating configured instances of {@link InLongMsgMixedFormatConverter}.
 */
public interface InLongMsgMixedFormatFactory {

    /**
     * Creates and configures a {@link AbstractInLongMsgMixedFormatConverter} using the given
     * properties.
     *
     * @param properties The normalized properties describing the format.
     * @return The configured serialization schema or null if the factory cannot
     * 	provide an instance of the class.
     */
    AbstractInLongMsgMixedFormatConverter createMixedFormatConverter(
            final Map<String, String> properties);

    /**
     * Creates and configures a {@link AbstractInLongMsgMixedFormatDeserializer} using the given
     * properties.
     *
     * @param properties The normalized properties describing the format.
     * @return The configured serialization schema or null if the factory cannot
     * 	provide an instance of the class.
     */
    AbstractInLongMsgMixedFormatDeserializer createMixedFormatDeserializer(
            final Map<String, String> properties);

    /**
     * Creates and configures a {@link AbstractInLongMsgMixedFormatConverter} using the given {@link
     * AbstractInLongMsgMixedFormatConverter.TableFormatContext}.
     *
     * @param context The context to create the instance of {@link
     *    AbstractInLongMsgMixedFormatConverter}.
     * @return The configured format converter, or null if the factory could not provide an
     * 	instance of the class.
     */
    AbstractInLongMsgMixedFormatConverter createMixedFormatConverter(
            final AbstractInLongMsgMixedFormatConverter.TableFormatContext context);

    /**
     * Creates and configures a {@link AbstractInLongMsgMixedFormatDeserializer} using the given {@link
     * AbstractInLongMsgMixedFormatDeserializer.TableFormatContext}.
     *
     * @param context The context to create the instance of {@link
     *    AbstractInLongMsgMixedFormatDeserializer}.
     * @return The configured deserializer, or null if the factory could not provide an instance of
     * 	the class.
     */
    AbstractInLongMsgMixedFormatDeserializer createMixedFormatDeserializer(
            final AbstractInLongMsgFormatDeserializer.TableFormatContext context);
}
