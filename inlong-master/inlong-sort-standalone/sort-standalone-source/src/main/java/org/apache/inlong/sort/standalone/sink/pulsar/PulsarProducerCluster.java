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

package org.apache.inlong.sort.standalone.sink.pulsar;

import org.apache.inlong.common.pojo.sort.node.PulsarNodeConfig;
import org.apache.inlong.sort.standalone.channel.ProfileEvent;
import org.apache.inlong.sort.standalone.config.holder.CommonPropertiesHolder;
import org.apache.inlong.sort.standalone.config.pojo.CacheClusterConfig;
import org.apache.inlong.sort.standalone.utils.Constants;
import org.apache.inlong.sort.standalone.utils.InlongLoggerFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.flume.Context;
import org.apache.flume.Transaction;
import org.apache.flume.lifecycle.LifecycleAware;
import org.apache.flume.lifecycle.LifecycleState;
import org.apache.pulsar.client.api.AuthenticationFactory;
import org.apache.pulsar.client.api.BatcherBuilder;
import org.apache.pulsar.client.api.ClientBuilder;
import org.apache.pulsar.client.api.CompressionType;
import org.apache.pulsar.client.api.HashingScheme;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.ProducerBuilder;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 *
 * PulsarProducerCluster
 */
public class PulsarProducerCluster implements LifecycleAware {

    public static final Logger LOG = InlongLoggerFactory.getLogger(PulsarProducerCluster.class);

    private static final String DEFAULT_COMPRESS_TYPE = "ZLIB";
    public static final String KEY_SERVICE_URL = "serviceUrl";
    public static final String KEY_AUTHENTICATION = "authentication";
    public static final String KEY_STATS_INTERVAL_SECONDS = "statsIntervalSeconds";

    public static final String KEY_ENABLEBATCHING = "enableBatching";
    public static final String KEY_BATCHINGMAXBYTES = "batchingMaxBytes";
    public static final String KEY_BATCHINGMAXMESSAGES = "batchingMaxMessages";
    public static final String KEY_BATCHINGMAXPUBLISHDELAY = "batchingMaxPublishDelay";
    public static final String KEY_MAXPENDINGMESSAGES = "maxPendingMessages";
    public static final String KEY_MAXPENDINGMESSAGESACROSSPARTITIONS = "maxPendingMessagesAcrossPartitions";
    public static final String KEY_SENDTIMEOUT = "sendTimeout";
    public static final String KEY_COMPRESSIONTYPE = "compressionType";
    public static final String KEY_BLOCKIFQUEUEFULL = "blockIfQueueFull";
    public static final String KEY_ROUNDROBINROUTERBATCHINGPARTITIONSWITCHFREQUENCY = "roundRobinRouter"
            + "BatchingPartitionSwitchFrequency";

    private final String workerName;
    private final PulsarFederationSinkContext sinkContext;
    private final PulsarNodeConfig nodeConfig;
    private final CacheClusterConfig cacheClusterConfig;
    private String cacheClusterName;
    private Context context;
    private LifecycleState state;
    private IEvent2PulsarRecordHandler handler;

    /**
     * pulsar client
     */
    private ClientBuilder clientBuilder;
    private PulsarClient client;
    private ProducerBuilder<byte[]> baseBuilder;

    private Map<String, Producer<byte[]>> producerMap = new ConcurrentHashMap<>();

    public PulsarProducerCluster(
            String workerName,
            CacheClusterConfig cacheClusterConfig,
            PulsarNodeConfig nodeConfig,
            PulsarFederationSinkContext context) {
        this.workerName = workerName;
        this.sinkContext = context;
        this.nodeConfig = nodeConfig;
        this.cacheClusterConfig = cacheClusterConfig;
        this.state = LifecycleState.IDLE;
        this.handler = sinkContext.createEventHandler();
    }

    /**
     * start
     */
    @Override
    public void start() {
        this.state = LifecycleState.START;
        try {
            // create pulsar client
            if (CommonPropertiesHolder.useUnifiedConfiguration()) {
                initBuilderByNodeConfig(nodeConfig);
            } else {
                initBuilderByCacheCluster(cacheClusterConfig);
            }

            this.client = clientBuilder
                    .statsInterval(context.getLong(KEY_STATS_INTERVAL_SECONDS, -1L), TimeUnit.SECONDS)
                    .build();

            // create producer template
            this.baseBuilder = client.newProducer();
            this.baseBuilder
                    .hashingScheme(HashingScheme.Murmur3_32Hash)
                    .enableBatching(context.getBoolean(KEY_ENABLEBATCHING, true))
                    .batchingMaxBytes(context.getInteger(KEY_BATCHINGMAXBYTES, 5242880))
                    .batchingMaxMessages(context.getInteger(KEY_BATCHINGMAXMESSAGES, 3000))
                    .batchingMaxPublishDelay(context.getInteger(KEY_BATCHINGMAXPUBLISHDELAY, 1),
                            TimeUnit.MILLISECONDS);
            this.baseBuilder.maxPendingMessages(context.getInteger(KEY_MAXPENDINGMESSAGES, 1000))
                    .maxPendingMessagesAcrossPartitions(
                            context.getInteger(KEY_MAXPENDINGMESSAGESACROSSPARTITIONS, 50000))
                    .sendTimeout(context.getInteger(KEY_SENDTIMEOUT, 0), TimeUnit.MILLISECONDS)
                    .compressionType(this.getPulsarCompressionType(context.getString(KEY_COMPRESSIONTYPE, "ZLIB")))
                    .blockIfQueueFull(context.getBoolean(KEY_BLOCKIFQUEUEFULL, true))
                    .roundRobinRouterBatchingPartitionSwitchFrequency(
                            context.getInteger(KEY_ROUNDROBINROUTERBATCHINGPARTITIONSWITCHFREQUENCY, 10))
                    .batcherBuilder(BatcherBuilder.DEFAULT);
        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private void initBuilderByCacheCluster(CacheClusterConfig cacheClusterConfig) {
        this.cacheClusterName = cacheClusterConfig.getClusterName();
        this.context = new Context(cacheClusterConfig.getParams());
        clientBuilder = PulsarClient.builder();
        String serviceUrl = cacheClusterConfig.getParams().get(KEY_SERVICE_URL);
        if (StringUtils.isBlank(serviceUrl)) {
            throw new IllegalArgumentException("service url should not be null");
        }

        clientBuilder.serviceUrl(serviceUrl);
        String authentication = cacheClusterConfig.getParams().get(KEY_AUTHENTICATION);
        if (StringUtils.isNoneBlank(authentication)) {
            clientBuilder.authentication(AuthenticationFactory.token(authentication));
        }
    }

    private void initBuilderByNodeConfig(PulsarNodeConfig nodeConfig) {
        this.cacheClusterName = nodeConfig.getNodeName();
        this.context = new Context(nodeConfig.getProperties() == null ? new HashMap<>() : nodeConfig.getProperties());

        clientBuilder = PulsarClient.builder();
        String serviceUrl = nodeConfig.getServiceUrl();
        if (StringUtils.isBlank(serviceUrl)) {
            throw new IllegalArgumentException("service url should not be null");
        }

        clientBuilder.serviceUrl(serviceUrl);
        String authentication = nodeConfig.getToken();
        if (StringUtils.isNoneBlank(authentication)) {
            clientBuilder.authentication(AuthenticationFactory.token(authentication));
        }
    }

    /**
     * getPulsarCompressionType
     *
     * @return CompressionType
     */
    private CompressionType getPulsarCompressionType(String type) {
        if (type == null) {
            return CompressionType.ZLIB;
        }

        switch (type.toUpperCase()) {
            case "LZ4":
                return CompressionType.LZ4;
            case "NONE":
                return CompressionType.NONE;
            case "ZLIB":
                return CompressionType.ZLIB;
            case "ZSTD":
                return CompressionType.ZSTD;
            case "SNAPPY":
                return CompressionType.SNAPPY;
            default:
                return CompressionType.NONE;
        }
    }

    /**
     * stop
     */
    @Override
    public void stop() {
        this.state = LifecycleState.STOP;
        // close producer
        for (Entry<String, Producer<byte[]>> entry : this.producerMap.entrySet()) {
            try {
                entry.getValue().close();
            } catch (PulsarClientException e) {
                LOG.error(e.getMessage(), e);
            }
        }
        try {
            this.client.close();
        } catch (PulsarClientException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /**
     * getLifecycleState
     *
     * @return
     */
    @Override
    public LifecycleState getLifecycleState() {
        return state;
    }

    /**
     * send
     *
     * @param  profileEvent
     * @param  tx
     * @return              boolean
     * @throws IOException
     */
    public boolean send(ProfileEvent profileEvent, Transaction tx) throws IOException {
        // send
        Map<String, String> headers = profileEvent.getHeaders();
        String topic = headers.get(Constants.TOPIC);
        // get producer
        Producer<byte[]> producer = this.producerMap.get(topic);
        if (producer == null) {
            try {
                LOG.debug("try to new a producer for topic " + topic);
                producer = baseBuilder.clone().topic(topic)
                        .producerName(workerName + "-" + cacheClusterName + "-" + topic + System.nanoTime())
                        .create();
                LOG.debug("create a new producer success:{}", producer.getProducerName());
                Producer<byte[]> oldProducer = this.producerMap.putIfAbsent(topic, producer);
                if (oldProducer != null) {
                    producer.close();
                    LOG.debug("close producer success:{}", producer.getProducerName());
                    producer = oldProducer;
                }
            } catch (Throwable ex) {
                LOG.error("create new producer failed", ex);
            }
        }
        // create producer failed
        if (producer == null) {
            tx.rollback();
            tx.close();
            sinkContext.addSendResultMetric(profileEvent, topic, false, System.currentTimeMillis());
            LOG.error("failed to create producer, send failed");
            throw new IllegalStateException();
        }

        // sendAsync
        byte[] sendBytes = this.handler.parse(sinkContext, profileEvent);
        // check
        if (sendBytes == null) {
            tx.commit();
            profileEvent.ack();
            tx.close();
            return true;
        }
        long sendTime = System.currentTimeMillis();
        CompletableFuture<MessageId> future = producer.newMessage()
                .properties(headers)
                .value(sendBytes)
                .sendAsync();
        // callback
        future.whenCompleteAsync((msgId, ex) -> {
            if (ex != null) {
                LOG.error("Send fail:{}", ex.getMessage());
                LOG.error(ex.getMessage(), ex);
                tx.rollback();
                tx.close();
                sinkContext.addSendResultMetric(profileEvent, topic, false, sendTime);
            } else {
                tx.commit();
                tx.close();
                sinkContext.addSendResultMetric(profileEvent, topic, true, sendTime);
                profileEvent.ack();
            }
        });
        return true;
    }

}
