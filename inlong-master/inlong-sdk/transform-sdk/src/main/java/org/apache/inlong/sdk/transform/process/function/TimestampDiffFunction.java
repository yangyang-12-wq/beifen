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
 * TimestampDiffFunction  -> TIMESTAMPDIFF(unit,datetime_expr1,datetime_expr2)
 * Description:
 * - returns NULL if datetime_expr1 or datetime_expr2 is NULL.
 * - returns datetime_expr2 − datetime_expr1, where datetime_expr1 and datetime_expr2 are date or datetime expressions.
 * The unit parameter: MICROSECOND (microseconds), SECOND, MINUTE, HOUR, DAY, WEEK, MONTH, QUARTER, or YEAR.
 */
@TransformFunction(names = {"timestamp_diff", "timestampdiff"})
public class TimestampDiffFunction implements ValueParser {

    private ValueParser unitParser;
    private ValueParser firstDateTimeParser;
    private ValueParser secondDateTimeParser;
    private static final DateTimeFormatter DEFAULT_FORMAT_DATE_MRO_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
    private static final DateTimeFormatter DEFAULT_FORMAT_DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DEFAULT_FORMAT_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public TimestampDiffFunction(Function expr) {
        List<Expression> expressions = expr.getParameters().getExpressions();
        unitParser = OperatorTools.buildParser(expressions.get(0));
        firstDateTimeParser = OperatorTools.buildParser(expressions.get(1));
        secondDateTimeParser = OperatorTools.buildParser(expressions.get(2));
    }

    @Override
    public Object parse(SourceData sourceData, int rowIndex, Context context) {
        Object unitObj = unitParser.parse(sourceData, rowIndex, context);
        Object firstDateTimeObj = firstDateTimeParser.parse(sourceData, rowIndex, context);
        Object secondDateTimeObj = secondDateTimeParser.parse(sourceData, rowIndex, context);
        if (firstDateTimeObj == null || secondDateTimeObj == null || unitObj == null) {
            return null;
        }
        String unit = OperatorTools.parseString(unitObj).toUpperCase();
        String firstDateTime = OperatorTools.parseString(firstDateTimeObj);
        String secondDateTime = OperatorTools.parseString(secondDateTimeObj);
        if (firstDateTime.isEmpty() || secondDateTime.isEmpty()) {
            return null;
        }
        try {
            LocalDateTime left = getLocalDate(firstDateTime);
            LocalDateTime right = getLocalDate(secondDateTime);
            switch (unit) {
                case "MICROSECOND":
                    return ChronoUnit.MICROS.between(left, right);
                case "SECOND":
                    return ChronoUnit.SECONDS.between(left, right);
                case "MINUTE":
                    return ChronoUnit.MINUTES.between(left, right);
                case "HOUR":
                    return ChronoUnit.HOURS.between(left, right);
                case "DAY":
                    return ChronoUnit.DAYS.between(left, right);
                case "WEEK":
                    return ChronoUnit.WEEKS.between(left, right);
                case "MONTH":
                    return ChronoUnit.MONTHS.between(left, right);
                case "QUARTER":
                    return ChronoUnit.MONTHS.between(left, right) / 3;
                case "YEAR":
                    return ChronoUnit.YEARS.between(left, right);
                default:
                    return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    public LocalDateTime getLocalDate(String dateString) {
        DateTimeFormatter formatter = null;
        LocalDateTime dateTime = null;
        if (dateString.indexOf('.') != -1) {
            formatter = DEFAULT_FORMAT_DATE_MRO_TIME;
            dateTime = LocalDateTime.parse(dateString, formatter);
        } else if (dateString.indexOf(' ') != -1) {
            formatter = DEFAULT_FORMAT_DATE_TIME;
            dateTime = LocalDateTime.parse(dateString, formatter);
        } else {
            formatter = DEFAULT_FORMAT_DATE;
            dateTime = LocalDate.parse(dateString, formatter).atStartOfDay();
        }
        return dateTime;
    }
}
