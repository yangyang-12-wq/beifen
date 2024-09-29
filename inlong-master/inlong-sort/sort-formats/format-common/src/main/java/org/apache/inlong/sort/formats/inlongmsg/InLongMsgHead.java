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

package org.apache.inlong.sort.formats.inlongmsg;

import org.apache.inlong.common.msg.InLongMsg;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The head deserialized from {@link InLongMsg}.
 */
public class InLongMsgHead implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The attributes in the head.
     */
    private final Map<String, String> attributes;

    /**
     * The interface of the record.
     */
    private final String streamId;

    /**
     * The time of the record.
     */
    private final Timestamp time;

    /**
     * The predefined fields extracted from the head.
     */
    private final List<String> predefinedFields;

    public InLongMsgHead(
            Map<String, String> attributes,
            String streamId,
            Timestamp time,
            List<String> predefinedFields) {
        this.attributes = attributes;
        this.streamId = streamId;
        this.time = time;
        this.predefinedFields = predefinedFields;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public String getStreamId() {
        return streamId;
    }

    public Timestamp getTime() {
        return time;
    }

    public List<String> getPredefinedFields() {
        return predefinedFields;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        InLongMsgHead that = (InLongMsgHead) o;
        return Objects.equals(attributes, that.attributes)
                && Objects.equals(streamId, that.streamId)
                && Objects.equals(time, that.time)
                && Objects.equals(predefinedFields, that.predefinedFields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributes, streamId, time, predefinedFields);
    }

    @Override
    public String toString() {
        return "InLongMsgHead{"
                + "attributes=" + attributes
                + ", streamId='" + streamId + '\''
                + ", time=" + time
                + ", predefinedFields=" + predefinedFields
                + '}';
    }
}
