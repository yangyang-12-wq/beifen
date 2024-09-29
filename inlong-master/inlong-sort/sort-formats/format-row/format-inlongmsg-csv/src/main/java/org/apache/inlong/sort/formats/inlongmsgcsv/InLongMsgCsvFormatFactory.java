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

package org.apache.inlong.sort.formats.inlongmsgcsv;

import org.apache.inlong.common.pojo.sort.dataflow.field.format.RowFormatInfo;
import org.apache.inlong.sort.formats.base.TableFormatDeserializer;
import org.apache.inlong.sort.formats.base.TableFormatDeserializerFactory;
import org.apache.inlong.sort.formats.inlongmsg.row.AbstractInLongMsgMixedFormatConverter;
import org.apache.inlong.sort.formats.inlongmsg.row.AbstractInLongMsgMixedFormatDeserializer;
import org.apache.inlong.sort.formats.inlongmsg.row.InLongMsgMixedFormatConverterValidator;
import org.apache.inlong.sort.formats.inlongmsg.row.InLongMsgMixedFormatDeserializerValidator;
import org.apache.inlong.sort.formats.inlongmsg.row.InLongMsgMixedFormatFactory;

import org.apache.flink.table.descriptors.DescriptorProperties;
import org.apache.flink.table.factories.TableFormatFactoryBase;
import org.apache.flink.types.Row;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.apache.inlong.sort.formats.base.TableFormatConstants.FORMAT_CHARSET;
import static org.apache.inlong.sort.formats.base.TableFormatConstants.FORMAT_DELIMITER;
import static org.apache.inlong.sort.formats.base.TableFormatConstants.FORMAT_ESCAPE_CHARACTER;
import static org.apache.inlong.sort.formats.base.TableFormatConstants.FORMAT_IGNORE_ERRORS;
import static org.apache.inlong.sort.formats.base.TableFormatConstants.FORMAT_LINE_DELIMITER;
import static org.apache.inlong.sort.formats.base.TableFormatConstants.FORMAT_NULL_LITERAL;
import static org.apache.inlong.sort.formats.base.TableFormatConstants.FORMAT_QUOTE_CHARACTER;
import static org.apache.inlong.sort.formats.base.TableFormatConstants.FORMAT_SCHEMA;
import static org.apache.inlong.sort.formats.inlongmsg.row.InLongMsgUtils.FORMAT_ATTRIBUTES_FIELD_NAME;
import static org.apache.inlong.sort.formats.inlongmsg.row.InLongMsgUtils.FORMAT_RETAIN_PREDEFINED_FIELD;
import static org.apache.inlong.sort.formats.inlongmsg.row.InLongMsgUtils.FORMAT_TIME_FIELD_NAME;
import static org.apache.inlong.sort.formats.inlongmsg.row.InLongMsgUtils.getDataRowFormatInfo;
import static org.apache.inlong.sort.formats.inlongmsgcsv.InLongMsgCsv.FORMAT_TYPE_VALUE;
import static org.apache.inlong.sort.formats.inlongmsgcsv.InLongMsgCsvUtils.FORMAT_DELETE_HEAD_DELIMITER;

/**
 * Table format factory for providing configured instances of InLongMsgCsv-to-row
 * serializer and deserializer.
 */
public final class InLongMsgCsvFormatFactory
        extends
            TableFormatFactoryBase<Row>
        implements
            TableFormatDeserializerFactory,
            InLongMsgMixedFormatFactory {

    public InLongMsgCsvFormatFactory() {
        super(FORMAT_TYPE_VALUE, 1, false);
    }

    @Override
    public List<String> supportedFormatProperties() {
        final List<String> properties = new ArrayList<>();
        properties.add(FORMAT_SCHEMA);
        properties.add(FORMAT_TIME_FIELD_NAME);
        properties.add(FORMAT_ATTRIBUTES_FIELD_NAME);
        properties.add(FORMAT_CHARSET);
        properties.add(FORMAT_DELIMITER);
        properties.add(FORMAT_LINE_DELIMITER);
        properties.add(FORMAT_ESCAPE_CHARACTER);
        properties.add(FORMAT_QUOTE_CHARACTER);
        properties.add(FORMAT_NULL_LITERAL);
        properties.add(FORMAT_IGNORE_ERRORS);
        properties.add(FORMAT_DELETE_HEAD_DELIMITER);
        properties.add(FORMAT_RETAIN_PREDEFINED_FIELD);

        return properties;
    }

    @Override
    public TableFormatDeserializer createFormatDeserializer(
            Map<String, String> properties) {
        final DescriptorProperties descriptorProperties =
                new DescriptorProperties(true);
        descriptorProperties.putProperties(properties);

        final InLongMsgCsvValidator validator = new InLongMsgCsvValidator();
        validator.validate(descriptorProperties);

        RowFormatInfo rowFormatInfo = getDataRowFormatInfo(descriptorProperties);

        InLongMsgCsvFormatDeserializer.Builder builder =
                new InLongMsgCsvFormatDeserializer.Builder(rowFormatInfo);
        builder.configure(descriptorProperties);

        return builder.build();
    }

    @Override
    public TableFormatDeserializer createFormatDeserializer(TableFormatDeserializer.TableFormatContext context) {
        TableFormatDeserializer deserializer =
                createFormatDeserializer(context.getFormatProperties());
        deserializer.init(context);
        return deserializer;
    }

    @Override
    public InLongMsgCsvMixedFormatDeserializer createMixedFormatDeserializer(
            Map<String, String> properties) {
        final DescriptorProperties descriptorProperties =
                new DescriptorProperties(true);
        descriptorProperties.putProperties(properties);

        final InLongMsgMixedFormatDeserializerValidator validator =
                new InLongMsgMixedFormatDeserializerValidator();
        validator.validate(descriptorProperties);

        final InLongMsgCsvMixedFormatDeserializer.Builder builder =
                new InLongMsgCsvMixedFormatDeserializer.Builder();
        builder.configure(descriptorProperties);

        return builder.build();
    }

    @Override
    public AbstractInLongMsgMixedFormatConverter createMixedFormatConverter(
            AbstractInLongMsgMixedFormatConverter.TableFormatContext context) {
        return createMixedFormatConverter(context.getFormatProperties());
    }

    @Override
    public AbstractInLongMsgMixedFormatDeserializer createMixedFormatDeserializer(
            TableFormatDeserializer.TableFormatContext context) {
        InLongMsgCsvMixedFormatDeserializer deserializer =
                createMixedFormatDeserializer(context.getFormatProperties());
        deserializer.init(context);
        return deserializer;
    }

    @Override
    public InLongMsgCsvMixedFormatConverter createMixedFormatConverter(
            Map<String, String> properties) {
        final DescriptorProperties descriptorProperties =
                new DescriptorProperties(true);
        descriptorProperties.putProperties(properties);

        final InLongMsgMixedFormatConverterValidator validator =
                new InLongMsgMixedFormatConverterValidator();
        validator.validate(descriptorProperties);

        final RowFormatInfo rowFormatInfo = getDataRowFormatInfo(descriptorProperties);

        final InLongMsgCsvMixedFormatConverter.Builder builder =
                new InLongMsgCsvMixedFormatConverter.Builder(rowFormatInfo);
        builder.configure(descriptorProperties);

        return builder.build();
    }
}
