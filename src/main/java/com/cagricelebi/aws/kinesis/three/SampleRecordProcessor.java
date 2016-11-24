package com.cagricelebi.aws.kinesis.three;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessorFactory;
import com.amazonaws.services.kinesis.clientlibrary.types.InitializationInput;
import com.amazonaws.services.kinesis.clientlibrary.types.ProcessRecordsInput;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownInput;
import com.amazonaws.services.kinesis.model.Record;
import com.cagricelebi.aws.kinesis.three.metric.PrometheusHelper;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cagricelebi
 */
public class SampleRecordProcessor implements IRecordProcessor {

    private static final Logger logger = LoggerFactory.getLogger(SampleRecordProcessor.class);
    private String shardId;
    private PrometheusHelper prometheus;

    @Override
    public void initialize(InitializationInput initializationInput) {
        this.shardId = initializationInput.getShardId();
        this.prometheus = new PrometheusHelper(shardId);
    }

    @Override
    public void processRecords(ProcessRecordsInput processRecordsInput) {
        try {
            logger.info("processRecords started ({}).", shardId);
            long start = System.currentTimeMillis(), scripttimer;
            List<Record> records = processRecordsInput.getRecords();

            for (Record record : records) {
                byte[] bytea = record.getData().array();
                // String recordString = new String(bytea, StandardCharsets.UTF_8).replace("\n", "").replace("\r", "");
                // logger.debug(recordString);
                prometheus(bytea, record.getApproximateArrivalTimestamp());
            }

            long scripttimerEmitComplete = System.currentTimeMillis() - start;
            scripttimer = System.currentTimeMillis();

            processRecordsInput.getCheckpointer().checkpoint();

            scripttimer = System.currentTimeMillis() - scripttimer;
            logger.info("processRecords completed in {} ms for {} records ({}). Checkpoint persist took +{} ms.",
                    scripttimerEmitComplete, records.size(), shardId, scripttimer);

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void prometheus(byte[] bytea, Date approxTime) {
        try {
            prometheus.calculateSize(bytea.length);
            if (approxTime != null) {
                prometheus.calculateApproxArrivalTimeDiff(approxTime.getTime());
            }
        } catch (Exception e) {
            // logger.error("Error during metric calculation of record: '{}'.", new String(bytea, StandardCharsets.UTF_8).replace("\n", "").replace("\r", ""));
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void shutdown(ShutdownInput shutdownInput) {
        try {
            logger.warn("(shutdown) {} is shutting down with reason: {}.", shardId, shutdownInput.getShutdownReason());
            logger.warn("(shutdown) checkpoint writing...");
            shutdownInput.getCheckpointer().checkpoint();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static class SampleRecordProcessorFactory implements IRecordProcessorFactory {

        @Override
        public IRecordProcessor createProcessor() {
            return new SampleRecordProcessor();
        }
    }

}
