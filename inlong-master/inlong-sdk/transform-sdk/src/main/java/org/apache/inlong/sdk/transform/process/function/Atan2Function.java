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

import net.sf.jsqlparser.expression.Function;

import java.math.BigDecimal;

/**
 * Atan2Function
 * description: atan2(numeric)--returns the arc tangent of a coordinate (numeric1, numeric2).
 */
@TransformFunction(names = {"atan2"})
public class Atan2Function implements ValueParser {

    private ValueParser xParser;
    private ValueParser yParser;

    public Atan2Function(Function expr) {
        xParser = OperatorTools.buildParser(expr.getParameters().getExpressions().get(0));
        yParser = OperatorTools.buildParser(expr.getParameters().getExpressions().get(1));
    }

    @Override
    public Object parse(SourceData sourceData, int rowIndex, Context context) {
        Object xObj = xParser.parse(sourceData, rowIndex, context);
        Object yObj = yParser.parse(sourceData, rowIndex, context);

        if (xObj == null) {
            throw new NullPointerException("Parsed number object on the x-axis is null");
        }

        BigDecimal xValue = OperatorTools.parseBigDecimal(xObj);
        BigDecimal yValue = OperatorTools.parseBigDecimal(yObj);

        return Math.atan2(xValue.doubleValue(), yValue.doubleValue());
    }
}
