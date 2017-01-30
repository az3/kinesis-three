package com.cagricelebi.aws.kinesis.three.metric;

import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cagricelebi
 */
public class PrometheusHelper {

    private static final Logger logger = LoggerFactory.getLogger(PrometheusHelper.class);

    private final String shardId;

    private static final Gauge prometheusApproxArrivalTimeGauge = Gauge.build()
            .name("kinesis_consumer_approximate_arrival_time_diff_seconds")
            .help("Approximate arrival time is a metadata inside a Kinesis record, this shows the difference between record.getApproximateArrivalTimestamp and consume time in seconds.")
            .labelNames("shard_id").register();

    private static final Histogram prometheusHistogram = Histogram.build()
            .buckets(400d, 1000d, 10000d)
            .name("kinesis_consumer_records")
            .help("Tracks the data passing though a shard, shows record size in bytes.")
            .labelNames("shard_id").register();

    public PrometheusHelper(String shardId) {
        this.shardId = shardId;
    }

    public void calculateSize(double recSize) {
        prometheusHistogram.labels(shardId).observe(recSize);
    }

    public void calculateApproxArrivalTimeDiff(long approximateArrivalTimestamp) {
        long diff = (System.currentTimeMillis() - approximateArrivalTimestamp) / 1000L;
        prometheusApproxArrivalTimeGauge.labels(shardId).set(diff);
    }

    public void shutdown() {
        prometheusHistogram.clear();
        prometheusApproxArrivalTimeGauge.clear();
    }
}
