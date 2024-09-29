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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * DateDiffFunction
 * description: DATEDIFF(d1, d2)
 * - return null if one of the two parameters is null or ""
 * - return null if one of the two parameters has an incorrect date format
 * - return the number of days between the dates d1->d2.
 */
@TransformFunction(names = {"datediff", "date_diff"})
public class DateDiffFunction implements ValueParser {

    private final ValueParser leftDateParser;
    private final ValueParser rightDateParser;
    private static final DateTimeFormatter DEFAULT_FORMAT_DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DEFAULT_FORMAT_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public DateDiffFunction(Function expr) {
        List<Expression> expressions = expr.getParameters().getExpressions();
        leftDateParser = OperatorTools.buildParser(expressions.get(0));
        rightDateParser = OperatorTools.buildParser(expressions.get(1));
    }

    @Override
    public Object parse(SourceData sourceData, int rowIndex, Context context) {
        Object leftDateObj = leftDateParser.parse(sourceData, rowIndex, context);
        Object rightDateObj = rightDateParser.parse(sourceData, rowIndex, context);
        if (leftDateObj == null || rightDateObj == null) {
            return null;
        }
        String leftDate = OperatorTools.parseString(leftDateObj);
        String rightDate = OperatorTools.parseString(rightDateObj);
        if (leftDate.isEmpty() || rightDate.isEmpty()) {
            return null;
        }
        try {
            LocalDate left = getLocalDate(leftDate);
            LocalDate right = getLocalDate(rightDate);
            return ChronoUnit.DAYS.between(right, left);
        } catch (Exception e) {
            return null;
        }
    }

    public LocalDate getLocalDate(String dateString) {
        DateTimeFormatter formatter = null;
        LocalDate dateTime = null;
        if (dateString.indexOf(' ') != -1) {
            formatter = DEFAULT_FORMAT_DATE_TIME;
            dateTime = LocalDateTime.parse(dateString, formatter).toLocalDate();
        } else {
            formatter = DEFAULT_FORMAT_DATE;
            dateTime = LocalDate.parse(dateString, formatter);
        }
        return dateTime;
    }
}
