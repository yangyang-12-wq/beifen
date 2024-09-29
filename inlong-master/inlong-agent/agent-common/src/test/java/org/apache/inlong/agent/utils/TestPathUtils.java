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

package org.apache.inlong.agent.utils;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;

import static org.apache.inlong.agent.utils.PathUtils.antPathIncluded;

public class TestPathUtils {

    @Test
    public void testAntPathIncluded() {
        if (File.separator.equals("\\")) {
            Assert.assertTrue(antPathIncluded("\\a\\b\\1\\3\\4\\5", "\\a\\*\\1\\3\\**\\1.log"));
            Assert.assertTrue(antPathIncluded("\\a\\b\\1\\3\\4\\5", "\\a\\*\\1\\3\\**\\1.log"));
            Assert.assertTrue(antPathIncluded("\\a\\b\\1\\3\\4\\5", "\\a\\*\\1\\3\\4\\5\\1.log"));
            Assert.assertTrue(antPathIncluded("\\a\\b\\1\\3\\4\\5", "\\a\\?\\1\\3\\4\\5\\1.log"));
            Assert.assertTrue(antPathIncluded("\\a\\b\\1\\3\\4\\5", "\\a\\*\\1\\3\\4\\5\\6\\1.log"));

            Assert.assertFalse(antPathIncluded("\\a\\b\\1\\3\\4\\5", "\\a\\c\\1\\3\\4\\5\\6\\1.log"));
            Assert.assertFalse(antPathIncluded("\\a\\b\\1\\3\\4\\5", "\\a\\*\\2\\3\\4\\5\\6\\1.log"));
        } else {
            Assert.assertTrue(antPathIncluded("/a/b/1/3/4/5", "/a/*/1/3/**/1.log"));
            Assert.assertTrue(antPathIncluded("/a/b/1/3/4/5", "/a/*/1/3/4/5/1.log"));
            Assert.assertTrue(antPathIncluded("/a/b/1/3/4/5", "/a/?/1/3/4/5/1.log"));
            Assert.assertTrue(antPathIncluded("/a/b/1/3/4/5", "/a/*/1/3/4/5/6/1.log"));

            Assert.assertFalse(antPathIncluded("/a/b/1/3/4/5", "/a/c/1/3/4/5/6/1.log"));
            Assert.assertFalse(antPathIncluded("/a/b/1/3/4/5", "/a/*/2/3/4/5/6/1.log"));
        }
    }
}
