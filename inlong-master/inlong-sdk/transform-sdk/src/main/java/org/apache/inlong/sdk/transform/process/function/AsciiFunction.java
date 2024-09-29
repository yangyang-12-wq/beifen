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

import java.util.List;

/**
 * ASCIIFunction
 * description: ASCII(string) -- Returns the numeric value of the first character of string. Returns NULL if string is NULL.
 */
@TransformFunction(names = {"ascii"})
public class AsciiFunction implements ValueParser {

    private final ValueParser stringParser;

    /**
     * Constructor
     * @param expr
     */
    public AsciiFunction(Function expr) {
        List<Expression> expressions = expr.getParameters().getExpressions();
        stringParser = OperatorTools.buildParser(expressions.get(0));
    }

    /**
     * parse
     * @param sourceData
     * @param rowIndex
     * @return
     */
    @Override
    public Object parse(SourceData sourceData, int rowIndex, Context context) {
        Object stringObj = stringParser.parse(sourceData, rowIndex, context);
        if (stringObj == null) {
            return null;
        }
        String str = OperatorTools.parseString(stringObj);
        if (str == null || str.isEmpty()) {
            return null;
        }
        return (int) str.charAt(0);
    }
}
