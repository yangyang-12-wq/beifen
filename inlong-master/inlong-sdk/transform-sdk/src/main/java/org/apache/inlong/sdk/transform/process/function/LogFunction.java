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

import java.math.BigDecimal;
import java.util.List;

/**
 * LogFunction
 * description: log(numeric) or log(numeric1, numeric2)--When called with one argument, returns the natural logarithm
 * of numeric. When called with two arguments, this function returns the logarithm of numeric2 to the base numeric1
 */
@TransformFunction(names = {"log"})
public class LogFunction implements ValueParser {

    private ValueParser baseParser;
    private final ValueParser numberParser;

    /**
     * Constructor
     * @param expr
     */
    public LogFunction(Function expr) {
        List<Expression> expressions = expr.getParameters().getExpressions();
        // Determine the number of arguments and build parser
        if (expressions.size() == 1) {
            numberParser = OperatorTools.buildParser(expressions.get(0));
        } else {
            baseParser = OperatorTools.buildParser(expressions.get(0));
            numberParser = OperatorTools.buildParser(expressions.get(1));
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
        Object numberObj = numberParser.parse(sourceData, rowIndex, context);
        BigDecimal numberValue = OperatorTools.parseBigDecimal(numberObj);
        if (baseParser != null) {
            Object baseObj = baseParser.parse(sourceData, rowIndex, context);
            BigDecimal baseValue = OperatorTools.parseBigDecimal(baseObj);
            return Math.log(numberValue.doubleValue()) / Math.log(baseValue.doubleValue());
        } else {
            return Math.log(numberValue.doubleValue());
        }
    }
}
