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

package org.apache.inlong.sort.formats.inlongmsgtlogcsv;

import org.apache.inlong.common.pojo.sort.dataflow.field.format.RowFormatInfo;
import org.apache.inlong.sort.formats.base.FieldToRowDataConverters;
import org.apache.inlong.sort.formats.base.FieldToRowDataConverters.FieldToRowDataConverter;
import org.apache.inlong.sort.formats.base.TableFormatUtils;
import org.apache.inlong.sort.formats.base.TextFormatBuilder;
import org.apache.inlong.sort.formats.inlongmsg.AbstractInLongMsgFormatDeserializer;
import org.apache.inlong.sort.formats.inlongmsg.FailureHandler;
import org.apache.inlong.sort.formats.inlongmsg.InLongMsgBody;
import org.apache.inlong.sort.formats.inlongmsg.InLongMsgHead;
import org.apache.inlong.sort.formats.inlongmsg.InLongMsgUtils;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.apache.inlong.sort.formats.base.TableFormatConstants.DEFAULT_DELIMITER;
import static org.apache.inlong.sort.formats.inlongmsg.InLongMsgUtils.DEFAULT_ATTRIBUTES_FIELD_NAME;
import static org.apache.inlong.sort.formats.inlongmsg.InLongMsgUtils.DEFAULT_TIME_FIELD_NAME;

/**
 * The deserializer for the records in InLongMsgTlogCsv format.
 */
public final class InLongMsgTlogCsvFormatDeserializer extends AbstractInLongMsgFormatDeserializer {

    private static final long serialVersionUID = 1L;

    /**
     * Format information describing the result type.
     */
    @Nonnull
    private final RowFormatInfo rowFormatInfo;

    /**
     * The name of the time field.
     */
    @Nullable
    private final String timeFieldName;

    /**
     * The name of the attributes field.
     */
    @Nullable
    private final String attributesFieldName;

    /**
     * The charset of the text.
     */
    private final String charset;

    /**
     * The delimiter between fields.
     */
    @Nonnull
    private final Character delimiter;

    /**
     * Escape character. Null if escaping is disabled.
     */
    @Nullable
    private final Character escapeChar;

    /**
     * Quote character. Null if quoting is disabled.
     */
    @Nullable
    private final Character quoteChar;

    @Nonnull
    private Boolean isIncludeFirstSegment = false;
    /**
     * The literal represented null values, default "".
     */
    @Nullable
    private final String nullLiteral;

    private final List<String> metadataKeys;

    private final FieldToRowDataConverter[] converters;

    public InLongMsgTlogCsvFormatDeserializer(
            @Nonnull RowFormatInfo rowFormatInfo,
            @Nullable String timeFieldName,
            @Nullable String attributesFieldName,
            @Nonnull String charset,
            @Nonnull Character delimiter,
            @Nullable Character escapeChar,
            @Nullable Character quoteChar,
            @Nullable String nullLiteral,
            List<String> metadataKeys,
            @Nonnull Boolean ignoreErrors) {
        this(
                rowFormatInfo,
                timeFieldName,
                attributesFieldName,
                charset,
                delimiter,
                escapeChar,
                quoteChar,
                nullLiteral,
                metadataKeys,
                false,
                InLongMsgUtils.getDefaultExceptionHandler(ignoreErrors));
    }

    public InLongMsgTlogCsvFormatDeserializer(
            @Nonnull RowFormatInfo rowFormatInfo,
            @Nullable String timeFieldName,
            @Nullable String attributesFieldName,
            @Nonnull String charset,
            @Nonnull Character delimiter,
            @Nullable Character escapeChar,
            @Nullable Character quoteChar,
            @Nullable String nullLiteral,
            List<String> metadataKeys,
            @Nonnull Boolean isIncludeFirstSegment,
            @Nonnull FailureHandler failureHandler) {
        super(failureHandler);

        this.rowFormatInfo = rowFormatInfo;
        this.timeFieldName = timeFieldName;
        this.attributesFieldName = attributesFieldName;
        this.charset = charset;
        this.delimiter = delimiter;
        this.escapeChar = escapeChar;
        this.quoteChar = quoteChar;
        this.nullLiteral = nullLiteral;
        this.metadataKeys = metadataKeys;
        this.isIncludeFirstSegment = isIncludeFirstSegment;
        converters = Arrays.stream(rowFormatInfo.getFieldFormatInfos())
                .map(formatInfo -> FieldToRowDataConverters.createConverter(
                        TableFormatUtils.deriveLogicalType(formatInfo)))
                .toArray(FieldToRowDataConverter[]::new);
    }

    @Override
    public TypeInformation<RowData> getProducedType() {
        return InLongMsgUtils.decorateRowTypeWithNeededHeadFieldsAndMetadata(
                timeFieldName,
                attributesFieldName,
                rowFormatInfo,
                metadataKeys);
    }

    @Override
    protected InLongMsgHead parseHead(String attr) throws Exception {
        return InLongMsgTlogCsvUtils.parseHead(attr);
    }

    @Override
    protected List<InLongMsgBody> parseBodyList(byte[] bytes) throws Exception {
        return Collections.singletonList(
                InLongMsgTlogCsvUtils.parseBody(bytes, charset, delimiter, escapeChar,
                        quoteChar, isIncludeFirstSegment));
    }

    @Override
    protected List<RowData> convertRowDataList(InLongMsgHead head, InLongMsgBody body) throws Exception {
        GenericRowData dataRow =
                InLongMsgTlogCsvUtils.deserializeRowData(
                        rowFormatInfo,
                        nullLiteral,
                        head.getPredefinedFields(),
                        body.getFields(),
                        converters);

        GenericRowData genericRowData = (GenericRowData) InLongMsgUtils.decorateRowDataWithNeededHeadFields(
                timeFieldName,
                attributesFieldName,
                head.getTime(),
                head.getAttributes(),
                dataRow);

        return Collections.singletonList(InLongMsgUtils.decorateRowWithMetaData(genericRowData, head, metadataKeys));
    }

    /**
     * The builder for {@link InLongMsgTlogCsvFormatDeserializer}.
     */
    public static class Builder extends TextFormatBuilder<Builder> {

        private String timeFieldName = DEFAULT_TIME_FIELD_NAME;
        private String attributesFieldName = DEFAULT_ATTRIBUTES_FIELD_NAME;
        private Character delimiter = DEFAULT_DELIMITER;
        private List<String> metadataKeys = Collections.emptyList();
        private boolean isIncludeFirstSegment = false;

        public Builder(RowFormatInfo rowFormatInfo) {
            super(rowFormatInfo);
        }

        public Builder setTimeFieldName(String timeFieldName) {
            this.timeFieldName = timeFieldName;
            return this;
        }

        public Builder setAttributesFieldName(String attributesFieldName) {
            this.attributesFieldName = attributesFieldName;
            return this;
        }

        public Builder setDelimiter(Character delimiter) {
            this.delimiter = delimiter;
            return this;
        }

        public Builder setMetadataKeys(List<String> metadataKeys) {
            this.metadataKeys = metadataKeys;
            return this;
        }

        public Builder setIncludeFirstSegment(boolean isIncludeFirstSegment) {
            this.isIncludeFirstSegment = isIncludeFirstSegment;
            return this;
        }

        public InLongMsgTlogCsvFormatDeserializer build() {
            return new InLongMsgTlogCsvFormatDeserializer(
                    rowFormatInfo,
                    timeFieldName,
                    attributesFieldName,
                    charset,
                    delimiter,
                    escapeChar,
                    quoteChar,
                    nullLiteral,
                    metadataKeys,
                    isIncludeFirstSegment,
                    InLongMsgUtils.getDefaultExceptionHandler(ignoreErrors));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        if (!super.equals(o)) {
            return false;
        }

        InLongMsgTlogCsvFormatDeserializer that = (InLongMsgTlogCsvFormatDeserializer) o;
        return rowFormatInfo.equals(that.rowFormatInfo) &&
                Objects.equals(timeFieldName, that.timeFieldName) &&
                Objects.equals(attributesFieldName, that.attributesFieldName) &&
                Objects.equals(charset, that.charset) &&
                delimiter.equals(that.delimiter) &&
                Objects.equals(escapeChar, that.escapeChar) &&
                Objects.equals(quoteChar, that.quoteChar) &&
                Objects.equals(nullLiteral, that.nullLiteral) &&
                Objects.equals(metadataKeys, that.metadataKeys) &&
                Objects.equals(isIncludeFirstSegment, that.isIncludeFirstSegment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), rowFormatInfo, timeFieldName,
                attributesFieldName, charset, delimiter, escapeChar, quoteChar,
                nullLiteral, metadataKeys, isIncludeFirstSegment);
    }
}
