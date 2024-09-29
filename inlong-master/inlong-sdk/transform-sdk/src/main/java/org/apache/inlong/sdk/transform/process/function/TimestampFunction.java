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
import org.apache.inlong.sdk.transform.process.utils.DateUtil;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * TimestampFunction  -> timestamp(expr1[, expr2])
 * description:
 * - return NULL if expr1 or expr2 is NULL.
 * - return the date or datetime expression expr as a datetime value if there is only one parameter
 * - return the result of the date or date time expression expr1 plus the time expression expr2 if there are two parameters
 */
@TransformFunction(names = {"timestamp"})
public class TimestampFunction implements ValueParser {

    private ValueParser dateTimeExprParser;
    private ValueParser timeExprParser;

    public TimestampFunction(Function expr) {
        List<Expression> expressions = expr.getParameters().getExpressions();
        dateTimeExprParser = OperatorTools.buildParser(expressions.get(0));
        if (expressions.size() == 2) {
            timeExprParser = OperatorTools.buildParser(expressions.get(1));
        }
    }

    @Override
    public Object parse(SourceData sourceData, int rowIndex, Context context) {
        Object dateTimeExprObj = dateTimeExprParser.parse(sourceData, rowIndex, context);
        if (dateTimeExprObj == null) {
            return null;
        }
        String dateTimeStr = dateTimeExprObj.toString();
        LocalDateTime localDateTime = DateUtil.parseLocalDateTime(dateTimeExprObj.toString());
        if (localDateTime == null) {
            // Not meeting the format requirements
            return null;
        }
        boolean hasMicroSecond = dateTimeStr.indexOf('.') != -1;
        String formatStr = DateUtil.YEAR_TO_SECOND;
        // Support the second parameter
        if (timeExprParser != null) {
            Object timeExprObj = timeExprParser.parse(sourceData, rowIndex, context);
            if (timeExprObj != null) {
                String timeStr = timeExprObj.toString();
                LocalTime localTime = DateUtil.parseLocalTime(timeStr);
                if (localTime == null) {
                    // Not meeting the format requirements
                    return null;
                }
                hasMicroSecond |= timeStr.indexOf('.') != -1;
                localDateTime = DateUtil.dateAdd(localDateTime, localTime);
            } else {
                return null;
            }
        }
        if (hasMicroSecond) {
            formatStr = DateUtil.YEAR_TO_MICRO;
        }
        return localDateTime.format(DateUtil.getDateTimeFormatter(formatStr));
    }
}