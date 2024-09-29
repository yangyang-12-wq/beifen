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

package org.apache.inlong.sort.formats.common;

import org.apache.inlong.common.pojo.sort.dataflow.field.format.DoubleFormatInfo;
import org.apache.inlong.common.pojo.sort.dataflow.field.format.FormatInfo;

import org.junit.Test;

import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link DoubleFormatInfo}.
 */
public class DoubleFormatInfoTest extends FormatInfoTestBase {

    @Override
    Collection<FormatInfo> createFormatInfos() {
        return Collections.singletonList(DoubleFormatInfo.INSTANCE);
    }

    @Test
    public void testSerialize() {
        assertEquals(
                "123.123456789",
                DoubleFormatInfo.INSTANCE.serialize(123.123456789));
    }

    @Test
    public void testDeserialize() {
        assertEquals(
                Double.valueOf(123.123456789),
                DoubleFormatInfo.INSTANCE.deserialize("123.123456789"));
    }
}
