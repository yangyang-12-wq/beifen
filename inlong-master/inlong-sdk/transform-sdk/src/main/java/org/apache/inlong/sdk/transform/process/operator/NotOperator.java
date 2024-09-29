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

package org.apache.inlong.sdk.transform.process.operator;

import org.apache.inlong.sdk.transform.decode.SourceData;
import org.apache.inlong.sdk.transform.process.Context;

import net.sf.jsqlparser.expression.NotExpression;

/**
 * NotOperator
 * 
 */
@TransformOperator(values = NotExpression.class)
public class NotOperator implements ExpressionOperator {

    private final ExpressionOperator node;

    public NotOperator(NotExpression expr) {
        this.node = OperatorTools.buildOperator(expr.getExpression());
    }

    /**
     * check
     * @param sourceData
     * @param rowIndex
     * @return
     */
    @Override
    public boolean check(SourceData sourceData, int rowIndex, Context context) {
        return !this.node.check(sourceData, rowIndex, context);
    }

}
