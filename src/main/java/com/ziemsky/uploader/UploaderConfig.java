package com.ziemsky.uploader;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.channel.MessageChannels;
import org.springframework.integration.dsl.channel.QueueChannelSpec;
import org.springframework.integration.file.dsl.Files;
import org.springframework.integration.handler.GenericHandler;
import org.springframework.integration.scheduling.PollerMetadata;

import java.io.File;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.springframework.integration.dsl.Pollers.fixedDelay;

@Configuration
@EnableIntegration
public class UploaderConfig {

    private static final int BATCH_SIZE = 10;

    public static final String FILES_QUEUE_CHANNEL = "filesQueueChannel";
    public static final String BATCH_QUEUE_CHANNEL = "batchQueueChannel";

    final Logger log = LoggerFactory.getLogger(UploaderConfig.class);

    private @Value("${java.io.tmpdir:'/tmp'}/inbound") Path inboundDir;

    // todo make the cameras rename the files to jpg after upload
    // to prevent files being picked up by uploader before they've been fully uploaded by
    // cameras

    // file polling endpoint
    //          |
    //  FILES_QUEUE_CHANNEL
    //          |
    //      aggregator
    //          |
    // BATCH_QUEUE_CHANNEL
    //          |
    // uploader activator
    //          |
    //      uploader

    @Bean IntegrationFlow inboundFileReaderEndpoint() {

        log.info("Monitoring {}", inboundDir);

        return IntegrationFlows.from(
            Files.inboundAdapter(
                // todo switch from polling to watch service? See docs for caveats!:
                // https://docs.spring.io/spring-integration/reference/html/files.html#watch-service-directory-scanner
                inboundDir.toFile(),
                Comparator.comparing(
                    File::getName,
                    (leftFileName, rightFileName) -> -1 * leftFileName.compareTo(rightFileName)
                ))
                .patternFilter("*.jpg")
        )
            .channel(FILES_QUEUE_CHANNEL)
            .get();
    }

    @Bean IntegrationFlow filesBatchAggregator() {
        return IntegrationFlows
            .from(FILES_QUEUE_CHANNEL)
            .aggregate(aggregatorSpec -> aggregatorSpec
                .correlationStrategy(message -> true)
                .releaseStrategy(group -> group.size() == BATCH_SIZE)
                .groupTimeout(1000)
                .sendPartialResultOnExpiry(true)
                .expireGroupsUponCompletion(true)
            )
            .channel(BATCH_QUEUE_CHANNEL)
            .get();
    }

    @Bean IntegrationFlow outboundFileUploaderEndpoint() {
        return IntegrationFlows
            .from(BATCH_QUEUE_CHANNEL)
            .handle(uploaderActivator())
            .get();
    }

    @NotNull private GenericHandler<List<File>> uploaderActivator() {
        return (payload, headers) -> {
            log.info("PAYLOAD: " + payload);
            log.info("HEADERS: " + headers);
            return null;
        };
    }

    @Bean QueueChannelSpec filesQueueChannel() {
        return MessageChannels.queue();
    }

    @Bean(name = PollerMetadata.DEFAULT_POLLER)
    PollerMetadata defaultPoller() {
        return fixedDelay(3, TimeUnit.SECONDS)
            .maxMessagesPerPoll(BATCH_SIZE)
            .get();
    }

}
