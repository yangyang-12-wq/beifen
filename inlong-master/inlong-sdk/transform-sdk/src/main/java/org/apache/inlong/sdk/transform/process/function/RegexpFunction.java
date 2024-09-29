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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * RegexpFunction
 * description: REGEX(string1, string2)--Returns TRUE if any (possibly empty) substring of string1 matches the Java
 *              regular expression string2, otherwise FALSE. Returns NULL if any of arguments is NULL.
 *              SIMILAR(string1, string2)--Same as above
 */
@TransformFunction(names = {"regex", "similar", "regexp_like"})
public class RegexpFunction implements ValueParser {

    private ValueParser inputParser;

    private ValueParser patternParser;

    public RegexpFunction(Function expr) {
        if (expr.getParameters() != null) {
            List<Expression> expressions = expr.getParameters().getExpressions();
            if (expressions != null && expressions.size() == 2) {
                inputParser = OperatorTools.buildParser(expressions.get(0));
                patternParser = OperatorTools.buildParser(expressions.get(1));
            }
        }
    }

    @Override
    public Object parse(SourceData sourceData, int rowIndex, Context context) {
        if (inputParser == null || patternParser == null) {
            return null;
        }
        String inputString = OperatorTools.parseString(inputParser.parse(sourceData, rowIndex, context));
        String patternString = OperatorTools.parseString(patternParser.parse(sourceData, rowIndex, context));
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(inputString);
        return matcher.find();
    }
}
