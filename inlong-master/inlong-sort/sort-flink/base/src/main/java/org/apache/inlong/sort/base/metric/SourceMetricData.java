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

package org.apache.inlong.sort.base.metric;

import org.apache.inlong.audit.AuditOperator;

import org.apache.flink.metrics.Counter;
import org.apache.flink.metrics.Gauge;
import org.apache.flink.metrics.Meter;
import org.apache.flink.metrics.MetricGroup;
import org.apache.flink.metrics.SimpleCounter;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import static org.apache.inlong.sort.base.Constants.CURRENT_EMIT_EVENT_TIME_LAG;
import static org.apache.inlong.sort.base.Constants.CURRENT_FETCH_EVENT_TIME_LAG;
import static org.apache.inlong.sort.base.Constants.NUM_BYTES_IN;
import static org.apache.inlong.sort.base.Constants.NUM_BYTES_IN_FOR_METER;
import static org.apache.inlong.sort.base.Constants.NUM_BYTES_IN_PER_SECOND;
import static org.apache.inlong.sort.base.Constants.NUM_RECORDS_IN;
import static org.apache.inlong.sort.base.Constants.NUM_RECORDS_IN_FOR_METER;
import static org.apache.inlong.sort.base.Constants.NUM_RECORDS_IN_PER_SECOND;
import static org.apache.inlong.sort.base.util.CalculateObjectSizeUtils.getDataSize;

/**
 * A collection class for handling metrics
 */
@Deprecated
public class SourceMetricData implements MetricData, Serializable, SourceMetricsReporter {

    private static final long serialVersionUID = 1L;
    private MetricGroup metricGroup;
    private final Map<String, String> labels;
    private Counter numRecordsIn;
    private Counter numBytesIn;
    private Counter numRecordsInForMeter;
    private Counter numBytesInForMeter;
    private Meter numRecordsInPerSecond;
    private Meter numBytesInPerSecond;
    private AuditOperator auditOperator;
    private List<Integer> auditKeys;

    /**
     * currentFetchEventTimeLag = FetchTime - messageTimestamp, where the FetchTime is the time the
     * record fetched into the source operator.
     */
    private Gauge currentFetchEventTimeLag;
    /**
     * currentEmitEventTimeLag = EmitTime - messageTimestamp, where the EmitTime is the time the record leaves the
     * source operator.
     */
    private Gauge currentEmitEventTimeLag;

    /**
     * fetchDelay = FetchTime - messageTimestamp, where the FetchTime is the time the
     * record fetched into the source operator.
     */
    private volatile long fetchDelay = 0L;

    /**
     * emitDelay = EmitTime - messageTimestamp, where the EmitTime is the time the record leaves the
     * source operator.
     */
    private volatile long emitDelay = 0L;

    public SourceMetricData(MetricOption option, MetricGroup metricGroup) {
        this.metricGroup = metricGroup;
        this.labels = option.getLabels();

        ThreadSafeCounter recordsInCounter = new ThreadSafeCounter();
        ThreadSafeCounter bytesInCounter = new ThreadSafeCounter();
        switch (option.getRegisteredMetric()) {
            default:
                recordsInCounter.inc(option.getInitRecords());
                bytesInCounter.inc(option.getInitBytes());
                registerMetricsForNumRecordsIn(recordsInCounter);
                registerMetricsForNumBytesIn(bytesInCounter);
                registerMetricsForNumBytesInForMeter(new ThreadSafeCounter());
                registerMetricsForNumRecordsInForMeter(new ThreadSafeCounter());
                registerMetricsForNumBytesInPerSecond();
                registerMetricsForNumRecordsInPerSecond();
                registerMetricsForCurrentFetchEventTimeLag();
                registerMetricsForCurrentEmitEventTimeLag();
                break;
        }

        if (option.getIpPorts().isPresent()) {
            AuditOperator.getInstance().setAuditProxy(option.getIpPortSet());
            this.auditOperator = AuditOperator.getInstance();
            this.auditKeys = option.getInlongAuditKeys();
        }
    }

    public SourceMetricData(MetricOption option) {
        this.labels = option.getLabels();

        if (option.getIpPorts().isPresent()) {
            AuditOperator.getInstance().setAuditProxy(option.getIpPortSet());
            this.auditOperator = AuditOperator.getInstance();
            this.auditKeys = option.getInlongAuditKeys();
        }
    }

    /**
     * Default counter is {@link SimpleCounter}
     * groupId and streamId and nodeId are label value, user can use it filter metric data when use metric reporter
     * prometheus
     */
    public void registerMetricsForNumRecordsInForMeter() {
        registerMetricsForNumRecordsInForMeter(new SimpleCounter());
    }

    /**
     * User can use custom counter that extends from {@link Counter}
     * groupId and streamId and nodeId are label value, user can use it filter metric data when use metric reporter
     * prometheus
     */
    public void registerMetricsForNumRecordsInForMeter(Counter counter) {
        numRecordsInForMeter = registerCounter(NUM_RECORDS_IN_FOR_METER, counter);
    }

    /**
     * Default counter is {@link SimpleCounter}
     * groupId and streamId and nodeId are label value, user can use it filter metric data when use metric reporter
     * prometheus
     */
    public void registerMetricsForNumBytesInForMeter() {
        registerMetricsForNumBytesInForMeter(new SimpleCounter());
    }

    /**
     * User can use custom counter that extends from {@link Counter}
     * groupId and streamId and nodeId are label value, user can use it filter metric data when use metric reporter
     * prometheus
     */
    public void registerMetricsForNumBytesInForMeter(Counter counter) {
        numBytesInForMeter = registerCounter(NUM_BYTES_IN_FOR_METER, counter);
    }

    /**
     * Default counter is {@link SimpleCounter}
     * groupId and streamId and nodeId are label value, user can use it filter metric data when use metric reporter
     * prometheus
     */
    public void registerMetricsForNumRecordsIn() {
        registerMetricsForNumRecordsIn(new SimpleCounter());
    }

    /**
     * User can use custom counter that extends from {@link Counter}
     * groupId and streamId and nodeId are label value, user can use it filter metric data when use metric reporter
     * prometheus
     */
    public void registerMetricsForNumRecordsIn(Counter counter) {
        numRecordsIn = registerCounter(NUM_RECORDS_IN, counter);
    }

    /**
     * Default counter is {@link SimpleCounter}
     * groupId and streamId and nodeId are label value, user can use it filter metric data when use metric reporter
     * prometheus
     */
    public void registerMetricsForNumBytesIn() {
        registerMetricsForNumBytesIn(new SimpleCounter());
    }

    /**
     * User can use custom counter that extends from {@link Counter}
     * groupId and streamId and nodeId are label value, user can use it filter metric data when use metric reporter
     * prometheus
     */
    public void registerMetricsForNumBytesIn(Counter counter) {
        numBytesIn = registerCounter(NUM_BYTES_IN, counter);
    }

    public void registerMetricsForNumRecordsInPerSecond() {
        numRecordsInPerSecond = registerMeter(NUM_RECORDS_IN_PER_SECOND, this.numRecordsInForMeter);
    }

    public void registerMetricsForNumBytesInPerSecond() {
        numBytesInPerSecond = registerMeter(NUM_BYTES_IN_PER_SECOND, this.numBytesInForMeter);
    }

    public void registerMetricsForCurrentFetchEventTimeLag() {
        currentFetchEventTimeLag = registerGauge(CURRENT_FETCH_EVENT_TIME_LAG, (Gauge<Long>) this::getFetchDelay);
    }

    public void registerMetricsForCurrentEmitEventTimeLag() {
        currentEmitEventTimeLag = registerGauge(CURRENT_EMIT_EVENT_TIME_LAG, (Gauge<Long>) this::getEmitDelay);
    }

    public Counter getNumRecordsIn() {
        return numRecordsIn;
    }

    public Counter getNumBytesIn() {
        return numBytesIn;
    }

    public Meter getNumRecordsInPerSecond() {
        return numRecordsInPerSecond;
    }

    public Meter getNumBytesInPerSecond() {
        return numBytesInPerSecond;
    }

    public Counter getNumRecordsInForMeter() {
        return numRecordsInForMeter;
    }

    public Counter getNumBytesInForMeter() {
        return numBytesInForMeter;
    }

    public long getFetchDelay() {
        return fetchDelay;
    }

    public long getEmitDelay() {
        return emitDelay;
    }

    @Override
    public MetricGroup getMetricGroup() {
        return metricGroup;
    }

    @Override
    public Map<String, String> getLabels() {
        return labels;
    }

    public void outputMetricsWithEstimate(Object data) {
        outputMetrics(1, getDataSize(data));
    }

    public void outputMetricsWithEstimate(Object data, long fetchDelay, long emitDelay) {
        outputMetrics(1, getDataSize(data));
        this.fetchDelay = fetchDelay;
        this.emitDelay = emitDelay;
    }

    @Override
    public void outputMetricsWithEstimate(Object data, long dataTime) {
        outputMetrics(1, getDataSize(data), dataTime);
    }

    public void outputMetrics(long rowCountSize, long rowDataSize) {
        outputDefaultMetrics(rowCountSize, rowDataSize);

        if (auditOperator != null) {
            for (Integer key : auditKeys) {
                auditOperator.add(
                        key,
                        getGroupId(),
                        getStreamId(),
                        System.currentTimeMillis(),
                        rowCountSize,
                        rowDataSize);
            }

        }
    }

    public void outputMetrics(long rowCountSize, long rowDataSize, long fetchDelay, long emitDelay) {
        outputDefaultMetrics(rowCountSize, rowDataSize, fetchDelay, emitDelay);

        if (auditOperator != null) {
            for (Integer key : auditKeys) {
                auditOperator.add(
                        key,
                        getGroupId(),
                        getStreamId(),
                        System.currentTimeMillis(),
                        rowCountSize,
                        rowDataSize);
            }

        }
    }

    public void outputMetrics(long rowCountSize, long rowDataSize, long dataTime) {
        outputDefaultMetrics(rowCountSize, rowDataSize);
        if (auditOperator != null) {
            for (Integer key : auditKeys) {
                auditOperator.add(
                        key,
                        getGroupId(),
                        getStreamId(),
                        getCurrentOrProvidedTime(dataTime),
                        rowCountSize,
                        rowDataSize);
            }
        }
    }

    private long getCurrentOrProvidedTime(long dataTime) {
        return dataTime == 0 ? System.currentTimeMillis() : dataTime;
    }

    private void outputDefaultMetrics(long rowCountSize, long rowDataSize) {
        if (numRecordsIn != null) {
            this.numRecordsIn.inc(rowCountSize);
        }

        if (numBytesIn != null) {
            this.numBytesIn.inc(rowDataSize);
        }

        if (numRecordsInForMeter != null) {
            this.numRecordsInForMeter.inc(rowCountSize);
        }

        if (numBytesInForMeter != null) {
            this.numBytesInForMeter.inc(rowDataSize);
        }
    }

    /**
     * flush audit data
     * usually call this method in close method or when checkpointing
     */
    public void flushAuditData() {
        if (auditOperator != null) {
            auditOperator.flush();
        }
    }

    private void outputDefaultMetrics(long rowCountSize, long rowDataSize, long fetchDelay, long emitDelay) {
        outputDefaultMetrics(rowCountSize, rowDataSize);
        this.fetchDelay = fetchDelay;
        this.emitDelay = emitDelay;
    }

    @Override
    public String toString() {
        return "SourceMetricData{"
                + "metricGroup=" + metricGroup
                + ", labels=" + labels
                + ", numRecordsIn=" + numRecordsIn.getCount()
                + ", numBytesIn=" + numBytesIn.getCount()
                + ", numRecordsInForMeter=" + numRecordsInForMeter.getCount()
                + ", numBytesInForMeter=" + numBytesInForMeter.getCount()
                + ", numRecordsInPerSecond=" + numRecordsInPerSecond.getRate()
                + ", numBytesInPerSecond=" + numBytesInPerSecond.getRate()
                + ", currentFetchEventTimeLag=" + currentFetchEventTimeLag.getValue()
                + ", currentEmitEventTimeLag=" + currentEmitEventTimeLag.getValue()
                + ", auditOperator=" + auditOperator
                + '}';
    }
}
