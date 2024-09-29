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

package org.apache.inlong.manager.pojo.transform.joiner;

import org.apache.inlong.manager.common.enums.TransformType;
import org.apache.inlong.manager.pojo.stream.StreamField;
import org.apache.inlong.manager.pojo.stream.StreamNode;
import org.apache.inlong.manager.pojo.transform.TransformDefinition;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * A class to define operations to join two streamNode in one with relation defined.
 * (A lookup join is typically used to enrich a table with data that is queried from an external system)
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
public class LookUpJoinerDefinition extends TransformDefinition {

    public LookUpJoinerDefinition(StreamNode leftNode,
            StreamNode rightNode,
            List<StreamField> leftJoinFields,
            List<StreamField> rightJoinFields) {
        this.transformType = TransformType.LOOKUP_JOINER;
        this.leftNode = leftNode;
        this.rightNode = rightNode;
        this.leftJoinFields = leftJoinFields;
        this.rightJoinFields = rightJoinFields;
    }

    /**
     * Left node for join
     */
    private StreamNode leftNode;

    /**
     * Right node for join
     */
    private StreamNode rightNode;

    /**
     * Join streamFields from left node
     */
    private List<StreamField> leftJoinFields;

    /**
     * Join streamFields from right node
     */
    private List<StreamField> rightJoinFields;

}
