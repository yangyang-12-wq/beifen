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

package org.apache.inlong.common.enums;

/**
 * Enum of instance state.
 */
public enum InstanceStateEnum {

    DEFAULT(0),
    FINISHED(1),
    DELETE(2);

    private final int state;

    InstanceStateEnum(int state) {
        this.state = state;
    }

    public static InstanceStateEnum getTaskState(int state) {
        switch (state) {
            case 0:
                return DEFAULT;
            case 1:
                return FINISHED;
            case 2:
                return DELETE;
            default:
                throw new RuntimeException("Unsupported instance state " + state);
        }
    }

    public int getState() {
        return state;
    }

}
