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

package org.apache.inlong.manager.pojo.source.pulsar;

import org.apache.inlong.manager.common.consts.SourceType;
import org.apache.inlong.manager.common.util.JsonTypeDefine;
import org.apache.inlong.manager.pojo.source.SourceRequest;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Pulsar source request
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "Pulsar source request")
@JsonTypeDefine(value = SourceType.PULSAR)
public class PulsarSourceRequest extends SourceRequest {

    @ApiModelProperty("Pulsar tenant")
    private String pulsarTenant = "default";

    @ApiModelProperty("Pulsar namespace")
    private String namespace;

    @ApiModelProperty("Pulsar topic")
    private String topic;

    @ApiModelProperty("Pulsar subscription")
    private String subscription;

    @ApiModelProperty("Pulsar adminUrl")
    private String adminUrl;

    @ApiModelProperty("Pulsar serviceUrl")
    private String serviceUrl;

    @ApiModelProperty("Primary key, needed when serialization type is csv, json, avro")
    private String primaryKey;

    @ApiModelProperty(value = "Data encoding format: UTF-8, GBK")
    private String dataEncoding;

    @ApiModelProperty(value = "Data separator")
    private String dataSeparator;

    @ApiModelProperty(value = "KV separator")
    private String kvSeparator;

    @ApiModelProperty(value = "Data field escape symbol")
    private String dataEscapeChar;

    @ApiModelProperty(value = "The message body wrap  wrap type, including: RAW, INLONG_MSG_V0, INLONG_MSG_V1, etc")
    private String wrapType;

    @ApiModelProperty("Configure the Source's startup mode."
            + " Available options are earliest, latest, external-subscription, and specific-offsets.")
    private String scanStartupMode = "earliest";

    @ApiModelProperty(value = "Client auth plugin class name")
    private String clientAuthPluginClassName;

    @ApiModelProperty(value = "Client auth params")
    private String clientAuthParams;

    @ApiModelProperty("Reset subscription time")
    private Long resetTime;

    public PulsarSourceRequest() {
        this.setSourceType(SourceType.PULSAR);
    }
}
