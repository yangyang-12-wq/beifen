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

package org.apache.inlong.dataproxy.config.pojo;

import org.apache.inlong.common.constant.MQType;

/**
 * cache cluster type
 */
public enum CacheType {

    TUBE(0, MQType.TUBEMQ),
    KAFKA(1, MQType.KAFKA),
    PULSAR(2, MQType.PULSAR),
    TDMQPULSAR(3, MQType.TDMQ_PULSAR),
    N(99, MQType.NONE);

    private final int id;
    private final String value;

    /**
     * 
     * Constructor
     * 
     * @param value
     */
    private CacheType(int id, String value) {
        this.id = id;
        this.value = value;
    }

    public int getId() {
        return id;
    }

    /**
     * value
     *
     * @return
     */
    public String value() {
        return this.value;
    }

    /**
     * toString
     */
    @Override
    public String toString() {
        return this.name() + ":" + this.value;
    }

    /**
     * convert
     *
     * @param  value
     * @return
     */
    public static CacheType convert(String value) {
        for (CacheType v : values()) {
            if (v.value().equalsIgnoreCase(value)) {
                return v;
            }
        }
        return N;
    }

    public static CacheType valueOf(int idValue) {
        for (CacheType v : values()) {
            if (v.getId() == idValue) {
                return v;
            }
        }
        return N;
    }
}
