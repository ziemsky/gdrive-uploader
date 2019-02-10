package com.ziemsky.uploader.conf;

import com.google.api.services.drive.Drive;
import com.ziemsky.uploader.FileRepository;
import com.ziemsky.uploader.GDriveFileRepository;
import com.ziemsky.uploader.SecurerService;
import com.ziemsky.uploader.google.drive.GDriveProvider;
import mu.KLogger;
import mu.KotlinLogging;
import org.springframework.boot.context.ApplicationPidFileWriter;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannelSpec;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.file.dsl.Files;
import org.springframework.integration.scheduling.PollerMetadata;

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.springframework.integration.dsl.Pollers.fixedDelay;

@Configuration
@EnableIntegration
@EnableConfigurationProperties(MutableUploaderConfigProperties.class)
public class UploaderConfig { // todo convert to Kotlin class

    private KLogger log = KotlinLogging.INSTANCE.logger(UploaderConfig.class.getName());

    private static final int BATCH_SIZE = 4;

    private static final String FILES_INCOMING_CHANNEL = "incomingFilesChannel";
    private static final String FILES_BATCHED_TO_SECURE_CHANNEL = "filesBatchedToSecureChannel";
    private static final int POLLING_INTERVAL_IN_MILLIS = 100;

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

    @Bean IntegrationFlow inboundFileReaderEndpoint(final UploaderConfigProperties config, final Environment env) {

        log.info("    spring.config.additional-location: {}", env.getProperty("spring.config.additional-location"));
        log.info("               spring.config.location: {}", env.getProperty("spring.config.location"));
        log.info("                               Config: {}", config);
        log.info("Monitoring folder for files to upload: {}", config.monitoring().path());

        return IntegrationFlows.from(
            Files.inboundAdapter(
                // todo switch from polling to watch service? See docs for caveats!:
                // https://docs.spring.io/spring-integration/reference/html/files.html#watch-service-directory-scanner
                config.monitoring().path().toFile(),
                Comparator.comparing(
                    File::getName,
                    (leftFileName, rightFileName) -> -1 * leftFileName.compareTo(rightFileName)
                ))
                .patternFilter("*.jpg")
        )
            .channel(FILES_INCOMING_CHANNEL)
            .get();
    }

    @Bean IntegrationFlow filesBatchAggregator() {
        return IntegrationFlows
            .from(FILES_INCOMING_CHANNEL)
            .aggregate(aggregatorSpec -> aggregatorSpec
                .correlationStrategy(message -> true)
                .releaseStrategy(group -> group.size() == BATCH_SIZE)
                .groupTimeout(1000)
                .sendPartialResultOnExpiry(true)
                .expireGroupsUponCompletion(true)
            )
            .channel(FILES_BATCHED_TO_SECURE_CHANNEL)
            .get();
    }

    @Bean IntegrationFlow outboundFileUploaderEndpoint(SecurerService securerService) {
        return IntegrationFlows
            .from(FILES_BATCHED_TO_SECURE_CHANNEL)
            .<List<File>>handle((payload, headers) -> {
                securerService.secure(payload);
                return null;
            })
            .get();
    }

    @Bean SecurerService securerService(final FileRepository fileRepository) {
        return new SecurerService(fileRepository);
    }

    @Bean Drive drive(final UploaderConfigProperties config) {
        return new GDriveProvider(
            config.google().drive().applicationUserName(),
            config.google().drive().tokensDirectory(),
            config.google().drive().credentialsFile(),
            config.google().drive().applicationName()
        ).drive();
    }

    @Bean FileRepository repository(final Drive drive) {

        return new GDriveFileRepository(drive);
    }

    @Bean MessageChannelSpec incomingFilesChannel() {
        return MessageChannels.direct();
    }

    @Bean(name = PollerMetadata.DEFAULT_POLLER)
    PollerMetadata defaultPoller() {
        return fixedDelay(POLLING_INTERVAL_IN_MILLIS, TimeUnit.MILLISECONDS)
            .maxMessagesPerPoll(BATCH_SIZE)
            .get();
    }

    @Bean ApplicationListener pidWriter() {
        final ApplicationPidFileWriter applicationPidFileWriter = new ApplicationPidFileWriter();
        applicationPidFileWriter.setTriggerEventType(ApplicationReadyEvent.class);
        // applicationPidFileWriter.setTriggerEventType(ApplicationStartedEvent.class); // todo report?

        return applicationPidFileWriter;
    }
}
