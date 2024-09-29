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

package org.apache.inlong.sdk.transform.process.function;

import org.apache.inlong.sdk.transform.decode.SourceData;
import org.apache.inlong.sdk.transform.process.Context;
import org.apache.inlong.sdk.transform.process.operator.OperatorTools;
import org.apache.inlong.sdk.transform.process.parser.ValueParser;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;

import java.util.ArrayList;
import java.util.List;

/**
 * ConcatFunction
 * 
 */
@TransformFunction(names = {"concat"})
public class ConcatFunction implements ValueParser {

    private List<ValueParser> nodeList;

    /**
     * Constructor
     * @param expr
     */
    public ConcatFunction(Function expr) {
        if (expr.getParameters() == null) {
            this.nodeList = new ArrayList<>();
        } else {
            List<Expression> params = expr.getParameters().getExpressions();
            nodeList = new ArrayList<>(params.size());
            for (Expression param : params) {
                ValueParser node = OperatorTools.buildParser(param);
                nodeList.add(node);
            }
        }
    }

    /**
     * parse
     * @param sourceData
     * @param rowIndex
     * @return
     */
    @Override
    public Object parse(SourceData sourceData, int rowIndex, Context context) {
        StringBuilder builder = new StringBuilder();
        for (ValueParser node : nodeList) {
            builder.append(node.parse(sourceData, rowIndex, context));
        }
        return builder.toString();
    }

}
