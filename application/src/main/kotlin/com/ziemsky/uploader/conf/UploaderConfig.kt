package com.ziemsky.uploader.conf

import com.google.api.services.drive.Drive
import com.ziemsky.uploader.FileRepository
import com.ziemsky.uploader.GDriveFileRepository
import com.ziemsky.uploader.SecurerService
import com.ziemsky.uploader.google.drive.GDriveProvider
import mu.KotlinLogging
import org.springframework.boot.context.ApplicationPidFileWriter
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.integration.config.EnableIntegration
import org.springframework.integration.dsl.IntegrationFlow
import org.springframework.integration.dsl.IntegrationFlows
import org.springframework.integration.dsl.MessageChannelSpec
import org.springframework.integration.dsl.MessageChannels
import org.springframework.integration.dsl.Pollers.fixedDelay
import org.springframework.integration.file.dsl.Files
import org.springframework.integration.scheduling.PollerMetadata
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

private val log = KotlinLogging.logger {}

@Configuration
@EnableIntegration
@EnableConfigurationProperties(MutableUploaderConfigProperties::class)
class UploaderConfig {

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

    @Bean
    internal fun inboundFileReaderEndpoint(config: UploaderConfigProperties, env: Environment): IntegrationFlow {

        log.info("    spring.config.additional-location: {}", env.getProperty("spring.config.additional-location"))
        log.info("               spring.config.location: {}", env.getProperty("spring.config.location"))
        log.info("                               Config: {}", config)
        log.info("Monitoring folder for files to upload: {}", config.monitoring().path())

        return IntegrationFlows.from(
                Files.inboundAdapter(
                        // todo switch from polling to watch service? See docs for caveats!:
                        // https://docs.spring.io/spring-integration/reference/html/files.html#watch-service-directory-scanner
                        config.monitoring().path().toFile(),
                        Comparator.comparing<File, String>(
                                { it.name },
                                { leftFileName, rightFileName -> -1 * leftFileName.compareTo(rightFileName) }
                        ))
                        .patternFilter("*.jpg")
        )
                .channel(FILES_INCOMING_CHANNEL)
                .get()
    }

    @Bean
    internal fun filesBatchAggregator(): IntegrationFlow {
        return IntegrationFlows
                .from(FILES_INCOMING_CHANNEL)
                .aggregate { aggregatorSpec ->
                    aggregatorSpec
                            .correlationStrategy { message -> true }
                            .releaseStrategy { group -> group.size() == BATCH_SIZE }
                            .groupTimeout(1000)
                            .sendPartialResultOnExpiry(true)
                            .expireGroupsUponCompletion(true)
                }
                .channel(FILES_BATCHED_TO_SECURE_CHANNEL)
                .get()
    }

    @Bean
    internal fun outboundFileUploaderEndpoint(securerService: SecurerService): IntegrationFlow {
        return IntegrationFlows
                .from(FILES_BATCHED_TO_SECURE_CHANNEL)
                .handle<List<File>> { payload, headers ->
                    securerService.secure(payload)
                    null
                }
                .get()
    }

    @Bean
    internal fun securerService(fileRepository: FileRepository): SecurerService {
        return SecurerService(fileRepository)
    }

    @Bean
    internal fun drive(config: UploaderConfigProperties): Drive {
        return GDriveProvider(
                config.google().drive().applicationUserName(),
                config.google().drive().tokensDirectory(),
                config.google().drive().credentialsFile(),
                config.google().drive().applicationName()
        ).drive()
    }

    @Bean
    internal fun repository(drive: Drive): FileRepository {

        return GDriveFileRepository(drive)
    }

    @Bean
    internal fun incomingFilesChannel(): MessageChannelSpec<*, *> {
        return MessageChannels.direct()
    }

    @Bean(name = [PollerMetadata.DEFAULT_POLLER])
    internal fun defaultPoller(): PollerMetadata {
        return fixedDelay(POLLING_INTERVAL_IN_MILLIS.toLong(), TimeUnit.MILLISECONDS)
                .maxMessagesPerPoll(BATCH_SIZE.toLong())
                .get()
    }

    @Bean
    internal fun pidWriter(): ApplicationListener<*> {
        val applicationPidFileWriter = ApplicationPidFileWriter()
        applicationPidFileWriter.setTriggerEventType(ApplicationReadyEvent::class.java)
        // applicationPidFileWriter.setTriggerEventType(ApplicationStartedEvent.class); // todo report?

        return applicationPidFileWriter
    }

    companion object {

        private val BATCH_SIZE = 4

        private val FILES_INCOMING_CHANNEL = "incomingFilesChannel"
        private val FILES_BATCHED_TO_SECURE_CHANNEL = "filesBatchedToSecureChannel"
        private val POLLING_INTERVAL_IN_MILLIS = 100
    }
}
