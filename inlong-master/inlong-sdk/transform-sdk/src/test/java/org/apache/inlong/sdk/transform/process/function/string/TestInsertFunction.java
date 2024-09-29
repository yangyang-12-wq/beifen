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

package org.apache.inlong.sdk.transform.process.function.string;

import org.apache.inlong.sdk.transform.decode.SourceDecoderFactory;
import org.apache.inlong.sdk.transform.encode.SinkEncoderFactory;
import org.apache.inlong.sdk.transform.pojo.TransformConfig;
import org.apache.inlong.sdk.transform.process.TransformProcessor;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;

public class TestInsertFunction extends AbstractFunctionStringTestBase {

    @Test
    public void testInsertFunction() throws Exception {
        String transformSql1 = "select insert(string1, numeric1, numeric2, string2) from source";
        TransformConfig config1 = new TransformConfig(transformSql1);
        TransformProcessor<String, String> processor1 = TransformProcessor
                .create(config1, SourceDecoderFactory.createCsvDecoder(csvSource),
                        SinkEncoderFactory.createKvEncoder(kvSink));

        // case1: insert('12345678', 3, 4, 'word') -> '12word78'
        List<String> output1 = processor1.transform("12345678|word|cloud|3|4|0", new HashMap<>());
        Assert.assertEquals(1, output1.size());
        Assert.assertEquals("result=12word78", output1.get(0));

        // case2: insert('12345678', -1, 4, 'word') -> '12345678'
        List<String> output2 = processor1.transform("12345678|word|cloud|-1|4|0", new HashMap<>());
        Assert.assertEquals(1, output2.size());
        Assert.assertEquals("result=12345678", output2.get(0));

        // case3: insert('12345678', 3, 100, 'word') -> '12word'
        List<String> output3 = processor1.transform("12345678|word|cloud|3|100|0", new HashMap<>());
        Assert.assertEquals(1, output3.size());
        Assert.assertEquals("result=12word", output3.get(0));

        // case4: insert('', 3, 4, 'word') -> ''
        List<String> output4 = processor1.transform("|word|cloud|3|4|0", new HashMap<>());
        Assert.assertEquals(1, output4.size());
        Assert.assertEquals("result=", output4.get(0));

        // case5: insert('12345678', 3, 4, '') -> '1278'
        List<String> output5 = processor1.transform("12345678||cloud|3|4|0", new HashMap<>());
        Assert.assertEquals(1, output5.size());
        Assert.assertEquals("result=1278", output5.get(0));
    }
}
