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

package org.apache.inlong.sdk.transform.process.parser;

import org.apache.inlong.sdk.transform.decode.SourceDecoderFactory;
import org.apache.inlong.sdk.transform.encode.SinkEncoderFactory;
import org.apache.inlong.sdk.transform.pojo.TransformConfig;
import org.apache.inlong.sdk.transform.process.TransformProcessor;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;

public class TestBitwiseLeftShiftParser extends AbstractParserTestBase {

    @Test
    public void testBitwiseLeftShiftParser() throws Exception {
        String transformSql = null, data = null;
        TransformConfig config = null;
        TransformProcessor<String, String> processor = null;
        List<String> output = null;

        // case1: 9223372036854775807 << 1
        transformSql = "select numeric1 << numeric2 from source";
        config = new TransformConfig(transformSql);
        processor = TransformProcessor
                .create(config, SourceDecoderFactory.createCsvDecoder(csvSource),
                        SinkEncoderFactory.createKvEncoder(kvSink));
        data = "9223372036854775807|1|3";
        output = processor.transform(data, new HashMap<>());
        Assert.assertEquals(1, output.size());
        Assert.assertEquals("result=18446744073709551614", output.get(0));

        // case2: 18446744073709551615 << 18446744073709551615
        data = "18446744073709551615|18446744073709551615|3";
        output = processor.transform(data, new HashMap<>());
        Assert.assertEquals(1, output.size());
        Assert.assertEquals("result=0", output.get(0));

        // case3: 9223372036854775807 << -1
        data = "9223372036854775807|-1|3";
        output = processor.transform(data, new HashMap<>());
        Assert.assertEquals(1, output.size());
        Assert.assertEquals("result=0", output.get(0));
    }
}
