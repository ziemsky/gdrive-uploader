package com.ziemsky.uploader.conf

import com.google.api.services.drive.Drive
import com.ziemsky.uploader.GDriveRemoteRepository
import com.ziemsky.uploader.Janitor
import com.ziemsky.uploader.RemoteRepository
import com.ziemsky.uploader.Securer
import com.ziemsky.uploader.google.drive.GDriveProvider
import com.ziemsky.uploader.model.local.LocalFile
import mu.KotlinLogging
import org.springframework.boot.context.ApplicationPidFileWriter
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.integration.config.EnableIntegration
import org.springframework.integration.dsl.*
import org.springframework.integration.dsl.Pollers.fixedDelay
import org.springframework.integration.file.dsl.Files
import org.springframework.integration.scheduling.PollerMetadata
import java.io.File
import java.nio.file.Paths
import java.time.LocalDate
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.function.Supplier

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
    //  RAW_FILES_INCOMING_CHANNEL
    //          |
    //     transformer
    //          |
    // LOCAL_FILES_TO_SECURE_CHANNEL
    //          |
    //       securer -> GDriveClient

    @Bean
    internal fun inboundFileReaderEndpoint(config: UploaderConfigProperties, env: Environment): IntegrationFlow {

        log.info("                          current dir: {}", Paths.get(".").toAbsolutePath())
        log.info("    spring.config.additional-location: {}", env.getProperty("spring.config.additional-location"))
        log.info("               spring.config.location: {}", env.getProperty("spring.config.location"))
        log.info("                               Config: {}", config)
        log.info("Monitoring folder for files to upload: {}", config.monitoring().path())

        return IntegrationFlows
                .from(
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
                .channel(RAW_FILES_INCOMING_CHANNEL)
                .get()
    }

    @Bean
    internal fun transformer(): IntegrationFlow = IntegrationFlows
            .from(RAW_FILES_INCOMING_CHANNEL)
            .transform(File::class.java, ::LocalFile)
            .channel(LOCAL_FILES_TO_SECURE_CHANNEL)
            .get()

    @Bean
    internal fun outboundFileUploaderEndpoint(securer: Securer): IntegrationFlow = IntegrationFlows
            .from(LOCAL_FILES_TO_SECURE_CHANNEL)
            .handle<LocalFile> { payload, _ ->
                securer.secure(payload)
                payload
            }
            .channel(SECURED_FILES_CHANNEL)
            .get()

    @Bean
    internal fun janitorEndpoint(janitor: Janitor): IntegrationFlow = IntegrationFlows
            .from(SECURED_FILES_CHANNEL)
            .handle<LocalFile> { payload, _ ->
                janitor.cleanupSecuredFile(payload)
            }
            .nullChannel()

//    @Scheduled(cron = "0 1 0 * * *")
//    @Bean
//    internal fun rotateDirs(janitor: Janitor) {
//        janitor.rotateRemoteDailyFolders()
//    }

    // todo make the flow read from a channel
    //  make cron scheduler send to the channel one message a day
    //  make application start send single message to the channel
    @Bean
    internal fun rotateRemoteDailyFolders(janitor: Janitor, config: UploaderConfigProperties): IntegrationFlow = IntegrationFlows
            .from(
                    Supplier { LocalDate.now() },
                    Consumer { e -> e.poller(Pollers.cron(config.rotation().cron())) }
            )
            .handle<LocalDate> { _, _ -> janitor.rotateRemoteDailyFolders() }
            .nullChannel()

    @Bean
    internal fun securerService(remoteRepository: RemoteRepository): Securer {
        return Securer(remoteRepository)
    }

    @Bean
    internal fun drive(config: UploaderConfigProperties): Drive {

        log.debug { "CURRENT DIR FROM UPLOADER CONFIG: ${Paths.get(".").toAbsolutePath()}" }

        return GDriveProvider(
                config.google().drive().applicationUserName(),
                config.google().drive().tokensDirectory(),
                config.google().drive().credentialsFile(),
                config.google().drive().applicationName()
        ).drive()
    }

    @Bean
    internal fun repository(drive: Drive): RemoteRepository {

        val gDriveRemoteRepository = GDriveRemoteRepository(drive)

        gDriveRemoteRepository.init()

        return gDriveRemoteRepository
    }

    @Bean
    internal fun janitor(remoteRepository: RemoteRepository, config: UploaderConfigProperties): Janitor {
        return Janitor(remoteRepository, config.rotation().maxDailyFolders())
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

        private val RAW_FILES_INCOMING_CHANNEL = "incomingFilesChannel"
        private val LOCAL_FILES_TO_SECURE_CHANNEL = "localFilesToSecureChannel"
        private val SECURED_FILES_CHANNEL = "securedFilesChannel"
        private val POLLING_INTERVAL_IN_MILLIS = 100
    }
}
