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
import java.util.regex.Pattern;

/**
 * RegexpReplaceFunction
 * description: REGEXP_REPLACE(string1, string2, string3)--Returns a string from STRING1 with all the substrings that
 *              match a regular expression STRING2 consecutively being replaced with STRING3.
 */
@TransformFunction(names = {"regexp_replace"})
public class RegexpReplaceFunction implements ValueParser {

    private ValueParser inputStringParser;

    private ValueParser patternStringParser;

    private ValueParser replaceStringParser;

    public RegexpReplaceFunction(Function expr) {
        if (expr.getParameters() != null) {
            List<Expression> expressions = expr.getParameters().getExpressions();
            if (expressions != null && expressions.size() >= 3) {
                inputStringParser = OperatorTools.buildParser(expressions.get(0));
                patternStringParser = OperatorTools.buildParser(expressions.get(1));
                replaceStringParser = OperatorTools.buildParser(expressions.get(2));
            }
        }
    }

    @Override
    public Object parse(SourceData sourceData, int rowIndex, Context context) {
        if (inputStringParser == null || patternStringParser == null || replaceStringParser == null) {
            return null;
        }
        String inputString = OperatorTools.parseString(inputStringParser.parse(sourceData, rowIndex, context));
        String patternString = OperatorTools.parseString(patternStringParser.parse(sourceData, rowIndex, context));
        String replaceString = OperatorTools.parseString(replaceStringParser.parse(sourceData, rowIndex, context));
        Pattern pattern = Pattern.compile(patternString);
        return pattern.matcher(inputString).replaceAll(replaceString);
    }
}
