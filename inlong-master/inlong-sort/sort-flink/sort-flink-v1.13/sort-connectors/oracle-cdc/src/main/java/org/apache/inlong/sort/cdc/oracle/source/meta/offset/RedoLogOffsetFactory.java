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

package org.apache.inlong.sort.cdc.oracle.source.meta.offset;

import org.apache.inlong.sort.cdc.base.source.meta.offset.Offset;
import org.apache.inlong.sort.cdc.base.source.meta.offset.OffsetFactory;

import java.util.Map;

/** An offset factory class create {@link RedoLogOffset} instance.
 *  Copy from com.ververica:flink-connector-oracle-cdc:2.3.0
 */
public class RedoLogOffsetFactory extends OffsetFactory {

    private static final long serialVersionUID = 1L;

    public RedoLogOffsetFactory() {
    }

    @Override
    public Offset newOffset(Map<String, String> offset) {
        return new RedoLogOffset(offset);
    }

    @Override
    public Offset newOffset(String filename, Long position) {
        throw new UnsupportedOperationException(
                "Do not support to create RedoLogOffset by filename and position.");
    }

    @Override
    public Offset newOffset(Long position) {
        throw new UnsupportedOperationException(
                "Do not support to create RedoLogOffset by position.");
    }

    @Override
    public Offset createInitialOffset() {
        return RedoLogOffset.INITIAL_OFFSET;
    }

    @Override
    public Offset createNoStoppingOffset() {
        return RedoLogOffset.NO_STOPPING_OFFSET;
    }
}
