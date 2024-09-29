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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * UnixTimestampFunction
 * description:
 * unix_timestamp(): returns current Unix timestamp in seconds
 * unix_timestamp(string1[, string2]): converts date time string string1 in format string2
 * (by default: yyyy-MM-dd HH:mm:ss if not specified) to Unix timestamp (in seconds)
 */
@TransformFunction(names = {"unix_timestamp"})
public class UnixTimestampFunction implements ValueParser {

    private ValueParser stringParser;
    private ValueParser formatParser;
    private static final String DEFAULT_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public UnixTimestampFunction(Function expr) {
        if (expr.getParameters() == null) {
            return;
        }
        List<Expression> expressions = expr.getParameters().getExpressions();

        // Determine the number of arguments and build parser
        stringParser = OperatorTools.buildParser(expressions.get(0));
        if (expressions.size() == 2) {
            formatParser = OperatorTools.buildParser(expressions.get(1));
        }
    }

    @Override
    public Object parse(SourceData sourceData, int rowIndex, Context context) {
        // If stringParser is null, return current Unix timestamp in seconds
        if (stringParser == null) {
            return Instant.now().getEpochSecond();
        }

        String dateString = OperatorTools.parseString(stringParser.parse(sourceData, rowIndex, context));
        String formatPattern =
                formatParser != null ? OperatorTools.parseString(formatParser.parse(sourceData, rowIndex, context))
                        : DEFAULT_FORMAT;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formatPattern);

        // Parse the input date string with the given format
        LocalDateTime dateTime = LocalDateTime.parse(dateString, formatter);

        // Convert LocalDateTime to Unix timestamp in seconds
        return dateTime.atZone(ZoneId.systemDefault()).toEpochSecond();
    }
}
