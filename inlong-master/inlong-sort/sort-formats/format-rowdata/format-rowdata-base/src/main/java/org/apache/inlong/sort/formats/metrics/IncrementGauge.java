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

package org.apache.inlong.sort.formats.metrics;

import org.apache.flink.metrics.Counter;
import org.apache.flink.metrics.Gauge;

public class IncrementGauge implements Gauge<Long> {

    private Counter counter;
    private long previousCount = 0;

    public IncrementGauge(Counter counter) {
        this.counter = counter;
    }

    @Override
    public Long getValue() {
        long currentCount = counter.getCount();
        long ret = currentCount - previousCount;
        previousCount = currentCount;
        return ret;
    }
}
