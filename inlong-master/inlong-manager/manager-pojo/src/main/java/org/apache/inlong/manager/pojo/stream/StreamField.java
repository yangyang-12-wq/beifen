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

package org.apache.inlong.manager.pojo.stream;

import org.apache.inlong.manager.common.tool.excel.annotation.ExcelEntity;
import org.apache.inlong.manager.common.tool.excel.annotation.ExcelField;
import org.apache.inlong.manager.common.tool.excel.annotation.Font;
import org.apache.inlong.manager.common.tool.excel.annotation.Style;
import org.apache.inlong.manager.common.tool.excel.validator.NonEmptyCellValidator;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.IndexedColors;

import java.io.Serializable;

/**
 * Stream filed, including field name, field type, etc.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("Stream field configuration")
@ExcelEntity(name = "InLong-StreamField-Template")
public class StreamField implements Serializable {

    @ApiModelProperty("Field index")
    private Integer id;

    @ApiModelProperty(value = "inlong group id", required = true)
    private String inlongGroupId;

    @ApiModelProperty(value = "inlong stream id", required = true)
    private String inlongStreamId;

    @ExcelField(name = "Field name", validator = NonEmptyCellValidator.class, font = @Font(size = 16), style = @Style(bgColor = IndexedColors.LIGHT_TURQUOISE, width = 9000, allBorderColor = IndexedColors.BLUE, allBorderStyle = BorderStyle.THIN), headerFont = @Font(size = 20, color = IndexedColors.WHITE), headerStyle = @Style(bgColor = IndexedColors.DARK_BLUE, width = 9000, allBorderColor = IndexedColors.BLUE, allBorderStyle = BorderStyle.THIN))
    @ApiModelProperty(value = "Field name", required = true)
    private String fieldName;

    @ExcelField(name = "Field type", validator = StreamFieldTypeCellValidator.class, font = @Font(size = 16), style = @Style(bgColor = IndexedColors.LIGHT_TURQUOISE, width = 6000, allBorderColor = IndexedColors.BLUE, allBorderStyle = BorderStyle.THIN), headerFont = @Font(size = 20, color = IndexedColors.WHITE), headerStyle = @Style(bgColor = IndexedColors.DARK_BLUE, width = 9000, allBorderColor = IndexedColors.BLUE, allBorderStyle = BorderStyle.THIN))
    @ApiModelProperty(value = "Field type", required = true)
    private String fieldType;

    @ExcelField(name = "Field comment", font = @Font(size = 16), style = @Style(bgColor = IndexedColors.LIGHT_TURQUOISE, width = 10000, allBorderColor = IndexedColors.BLUE, allBorderStyle = BorderStyle.THIN), headerFont = @Font(size = 20, color = IndexedColors.WHITE), headerStyle = @Style(bgColor = IndexedColors.DARK_BLUE, width = 9000, allBorderColor = IndexedColors.BLUE, allBorderStyle = BorderStyle.THIN))
    @ApiModelProperty("Field comment")
    private String fieldComment;

    @ApiModelProperty(value = "Is predefined field, 1: yes, 0: no")
    private Integer isPredefinedField;

    @ApiModelProperty(value = "Field value for constants")
    private String fieldValue;

    @ApiModelProperty(value = "Value expression of predefined field")
    private String preExpression;

    @ApiModelProperty("Is this field a meta field, 0: no, 1: yes")
    @Builder.Default
    private Integer isMetaField = 0;

    @ApiModelProperty(value = "Meta field name")
    private String metaFieldName;

    @ApiModelProperty("Field format, including: MICROSECONDS, MILLISECONDS, SECONDS, SQL, ISO_8601"
            + " and custom such as 'yyyy-MM-dd HH:mm:ss'. This is mainly used for time format")
    private String fieldFormat;

    @ApiModelProperty("Origin node name which stream fields belong")
    private String originNodeName;

    @ApiModelProperty("Origin field name before transform operation")
    private String originFieldName;

    @ApiModelProperty("Extra Param in JSON style")
    private String extParams;

    public StreamField(int index, String fieldType, String fieldName, String fieldComment, String fieldValue) {
        this.id = index;
        this.fieldType = fieldType;
        this.fieldName = fieldName;
        this.fieldComment = fieldComment;
        this.fieldValue = fieldValue;
        // default
        this.isMetaField = 0;
    }

    public StreamField(int index, String fieldType, String fieldName, String fieldComment, String fieldValue,
            Integer isMetaField, String metaFieldName) {
        this(index, fieldType, fieldName, fieldComment, fieldValue);
        this.isMetaField = isMetaField;
        this.metaFieldName = metaFieldName;
    }

    public StreamField(int index, String fieldType, String fieldName, String fieldComment, String fieldValue,
            Integer isMetaField, String metaFieldName, String originNodeName) {
        this(index, fieldType, fieldName, fieldComment, fieldValue);
        this.isMetaField = isMetaField;
        this.metaFieldName = metaFieldName;
        this.originNodeName = originNodeName;
    }

    public StreamField(int index, String fieldType, String fieldName, String fieldComment, String fieldValue,
            Integer isMetaField, String metaFieldName, String originNodeName, String originFieldName) {
        this(index, fieldType, fieldName, fieldComment, fieldValue);
        this.isMetaField = isMetaField;
        this.metaFieldName = metaFieldName;
        this.originNodeName = originNodeName;
        this.originFieldName = originFieldName;
    }

}
