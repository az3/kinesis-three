package com.cagricelebi.aws.kinesis.three;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker;
import com.cagricelebi.aws.kinesis.three.metric.PrometheusRunnable;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cagricelebi
 */
public class Starter {

    private static final Logger logger = LoggerFactory.getLogger(Starter.class);

    private ExecutorService kclWorkerExecutor;
    private Worker kclWorker;

    private ExecutorService prometheusExecutor;
    private static final int prometheusPort = 8090;
    private String prometheusShutdownKey; // shutdown key can be used for graceful shutdown, not implemented.

    public static void main(String[] args) {
        try {
            Starter s = new Starter();
            s.start();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void start() {
        try {
            prometheusExecutor = Executors.newSingleThreadExecutor();
            prometheusShutdownKey = new BigInteger(130, new SecureRandom()).toString(32);
            logger.info("Prometheus on port {} with shutdownKey: '{}'", prometheusPort, prometheusShutdownKey);
            prometheusExecutor.submit(new PrometheusRunnable(prometheusShutdownKey, prometheusPort));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        try {
            kclWorkerExecutor = Executors.newCachedThreadPool();
            kclWorker = new Worker.Builder().recordProcessorFactory(
                    new SampleRecordProcessor.SampleRecordProcessorFactory()
            ).config(getConfig()).build();
            logger.info("kclWorker constructed successfully.");
            kclWorkerExecutor.submit(kclWorker);
            logger.info("kclWorker submitted to kclWorkerExecutor.");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * Copied from amazon-kinesis-connectors.
     * https://github.com/awslabs/amazon-kinesis-connectors/blob/9b34900413a25212f4dec9d7f1e64136df31db9b/src/main/java/com/amazonaws/services/kinesis/connectors/KinesisConnectorConfiguration.java#L54
     *
     * @return
     */
    private KinesisClientLibConfiguration getConfig() {

        String configFile = System.getProperty("CONFIG_FILE");
        if (configFile == null || configFile.isEmpty()) {
            String msg = "Could not load properties file -DCONFIG_FILE from classpath";
            throw new IllegalStateException(msg);
        }

        Properties properties = new Properties();

        try (InputStream configStream = new FileInputStream(configFile)) {
            properties.load(configStream);
        } catch (Exception e) {
            String msg = "Could not load properties file -DCONFIG_FILE from classpath";
            throw new IllegalStateException(msg, e);
        }

        String appName = properties.getProperty("appName");
        String kinesisInputStream = properties.getProperty("kinesisInputStream");
        String workerID = properties.getProperty("workerID");
        String accessKey = properties.getProperty("accessKey");
        String secretKey = properties.getProperty("secretKey");

        BasicAWSCredentials basicAWSCredentials = new BasicAWSCredentials(accessKey, secretKey);
        AWSCredentialsProvider credentialsProvider = new AWSCredentialsProvider() {
            @Override
            public AWSCredentials getCredentials() {
                return basicAWSCredentials;
            }

            @Override
            public void refresh() {
            }
        };

        KinesisClientLibConfiguration config = new KinesisClientLibConfiguration(appName, kinesisInputStream, credentialsProvider, workerID);
        try {
            config.withRegionName(properties.getProperty("regionName"));
            config.withKinesisEndpoint(properties.getProperty("kinesisEndpoint"));
            config.withIdleTimeBetweenReadsInMillis(Long.parseLong(properties.getProperty("idleTimeBetweenReadsInMillis", "1000")));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return config;

    }

}
