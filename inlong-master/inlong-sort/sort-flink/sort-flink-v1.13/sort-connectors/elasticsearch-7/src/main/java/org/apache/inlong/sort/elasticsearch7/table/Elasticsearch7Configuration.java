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

package org.apache.inlong.sort.elasticsearch7.table;

import org.apache.inlong.sort.elasticsearch.ActionRequestFailureHandler;
import org.apache.inlong.sort.elasticsearch.table.ElasticsearchConfiguration;
import org.apache.inlong.sort.elasticsearch.utils.IgnoringFailureHandler;
import org.apache.inlong.sort.elasticsearch.utils.NoOpFailureHandler;
import org.apache.inlong.sort.elasticsearch7.utils.RetryRejectedExecutionFailureHandler;

import org.apache.flink.annotation.Internal;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.table.api.ValidationException;
import org.apache.flink.util.InstantiationUtil;
import org.apache.http.HttpHost;
import org.elasticsearch.action.DocWriteRequest;

import java.util.List;
import java.util.stream.Collectors;

import static org.apache.inlong.sort.elasticsearch.table.ElasticsearchOptions.FAILURE_HANDLER_OPTION;
import static org.apache.inlong.sort.elasticsearch.table.ElasticsearchOptions.HOSTS_OPTION;

/**
 * Elasticsearch 7 specific configuration.
 */
@Internal
final class Elasticsearch7Configuration extends ElasticsearchConfiguration {

    Elasticsearch7Configuration(ReadableConfig config, ClassLoader classLoader) {
        super(config, classLoader);
    }

    private static HttpHost validateAndParseHostsString(String host) {
        try {
            HttpHost httpHost = HttpHost.create(host);
            if (httpHost.getPort() < 0) {
                throw new ValidationException(
                        String.format(
                                "Could not parse host '%s' in option '%s'. It should follow the format 'http://host_name:port'. Missing port.",
                                host, HOSTS_OPTION.key()));
            }

            if (httpHost.getSchemeName() == null) {
                throw new ValidationException(
                        String.format(
                                "Could not parse host '%s' in option '%s'. It should follow the format 'http://host_name:port'. Missing scheme.",
                                host, HOSTS_OPTION.key()));
            }
            return httpHost;
        } catch (Exception e) {
            throw new ValidationException(
                    String.format(
                            "Could not parse host '%s' in option '%s'. It should follow the format 'http://host_name:port'.",
                            host, HOSTS_OPTION.key()),
                    e);
        }
    }

    public List<HttpHost> getHosts() {
        return config.get(HOSTS_OPTION).stream()
                .map(Elasticsearch7Configuration::validateAndParseHostsString)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public ActionRequestFailureHandler<DocWriteRequest<?>> getFailureHandler() {
        final ActionRequestFailureHandler<DocWriteRequest<?>> failureHandler;
        String value = config.get(FAILURE_HANDLER_OPTION);
        switch (value.toUpperCase()) {
            case "FAIL":
                failureHandler = new NoOpFailureHandler<>();
                break;
            case "IGNORE":
                failureHandler = new IgnoringFailureHandler<>();
                break;
            case "RETRY-REJECTED":
                failureHandler = new RetryRejectedExecutionFailureHandler();
                break;
            default:
                try {
                    Class<?> failureHandlerClass = Class.forName(value, false, getClassLoader());
                    failureHandler = (ActionRequestFailureHandler<DocWriteRequest<?>>) InstantiationUtil
                            .instantiate(failureHandlerClass);
                } catch (ClassNotFoundException e) {
                    throw new ValidationException(
                            "Could not instantiate the failure handler class: " + value, e);
                }
                break;
        }
        return failureHandler;
    }
}
