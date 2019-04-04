package com.ziemsky.uploader.conf

import com.google.api.services.drive.Drive
import com.ziemsky.uploader.*
import com.ziemsky.uploader.google.drive.GDriveProvider
import com.ziemsky.uploader.model.local.LocalFile
import com.ziemsky.uploader.model.repo.RepoFolderName
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.ApplicationPidFileWriter
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.integration.annotation.Gateway
import org.springframework.integration.annotation.IntegrationComponentScan
import org.springframework.integration.annotation.MessagingGateway
import org.springframework.integration.config.EnableIntegration
import org.springframework.integration.dsl.*
import org.springframework.integration.dsl.Pollers.fixedDelay
import org.springframework.integration.file.dsl.Files
import org.springframework.integration.scheduling.PollerMetadata
import java.io.File
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private val log = KotlinLogging.logger {}

@Configuration
@EnableIntegration
@EnableConfigurationProperties(MutableUploaderConfigProperties::class)
@IntegrationComponentScan
class UploaderConfig {

    // todo make the cameras rename the files to jpg after upload
    // to prevent files being picked up by uploader before they've been fully uploaded by
    // cameras



    // inboundAdapter: file
    //       |
    //   transform(File): LocalFile
    //       |
    //    securer.ensureRemoteDailyFolder(LocalFile)  -  janitor.rotateRemoteDailyFolders()
    //      | |
    //      | | executor channel
    //      | |
    //    securer.secure(LocalFile)
    //      | |
    //      | |
    //      | |
    //    securerEventReporter.notifyFileSecured(LocalFile)
    //      | |      | |
    //      | |      | |
    //      | |      | |
    //      | |    janitor.cleanupSecuredLocalFile(LocalFile)
    //      | |
    //      | |
    //    statsLogger.logStatsForSecuredFile(LocalFile)




    @Autowired
    lateinit var config: UploaderConfigProperties

    @Bean
    internal fun inboundFileReaderEndpoint(
            config: UploaderConfigProperties,
            env: Environment,
            securer: Securer,
            janitor: Janitor
    ): IntegrationFlow {

        // todo move logging out
        log.info("                          current dir: {}", Paths.get(".").toAbsolutePath())
        log.info("    spring.config.additional-location: {}", env.getProperty("spring.config.additional-location"))
        log.info("               spring.config.location: {}", env.getProperty("spring.config.location"))
        log.info("                               Config: {}", config)
        log.info("Monitoring folder for files to upload: {}", config.monitoring().path())

        return IntegrationFlows
                .from(
                        Files.inboundAdapter(
                                // todo switch from polling to watch service? See docs for caveats!:
                                //  https://docs.spring.io/spring-integration/reference/html/#watch-service-directory-scanner
                                config.monitoring().path().toFile(),
                                Comparator.comparing<File, String>(
                                        { it.name },
                                        { leftFileName, rightFileName -> -1 * leftFileName.compareTo(rightFileName) }
                                ))
                                .patternFilter("*.jpg")
                )

                .transform(File::class.java, ::LocalFile)

                .handle<LocalFile> { localFileToSecure, _ ->
                    securer.ensureRemoteDailyFolder(localFileToSecure)
                    localFileToSecure
                }

                .channel(executorChannel())

                .handle<LocalFile> { localFileToSecure, _ -> securer.secure(localFileToSecure) }

                .nullChannel()
    }

    @Bean
    internal fun rotateRemoteDailyFolders(
            janitor: Janitor,
            config: UploaderConfigProperties
    ): IntegrationFlow = IntegrationFlows
            .from(REMOTE_DAILY_FOLDER_CREATED_CHANNEL)
            .handle<RepoFolderName> { _, _ -> janitor.rotateRemoteDailyFolders() }
            .nullChannel()

//    @Bean
//    internal fun cleanupLocalSecuredFiles(janitor: Janitor): IntegrationFlow = IntegrationFlows
//                .from(SECURED_FILES_BATCHED_CHANNEL)
//                .handle<LocalFile> { securedLocalFile, _ -> janitor.cleanupSecuredFile(securedLocalFile) }
//                .nullChannel()
//
//    @Bean
//    internal fun logStats(statsLogger: StatsLogger): IntegrationFlow = IntegrationFlows
//            .from(SECURED_FILES_BATCHED_CHANNEL)
//            .handle<LocalFile> { securedLocalFile, _ -> statsLogger.logStatsForSecuredFile(securedLocalFile) }
//            .nullChannel()

    @Bean
    internal fun sg(janitor: Janitor, statsLogger: StatsLogger): IntegrationFlow {
        return IntegrationFlows.from(SECURED_FILES_CHANNEL)
                .scatterGather({ scatterer ->scatterer
                            .recipientFlow { flow -> flow.handle<LocalFile> { securedLocalFile, _ -> janitor.cleanupSecuredFile(securedLocalFile); securedLocalFile } }
                            .recipientFlow { flow -> flow.handle<LocalFile> { securedLocalFile, _ -> statsLogger.logStatsForSecuredFile(securedLocalFile); securedLocalFile } }
                },
                        { gatherer ->
                            gatherer
                                    .correlationStrategy { message -> true }
                                    .releaseStrategy { messageGroup -> true }
                        }
                ).nullChannel()
    }


//    @Bean
//    internal fun filesBatchAggregator(statsLogger: StatsLogger, janitor: Janitor): IntegrationFlow = IntegrationFlows
//            .from(SECURED_FILES_CHANNEL)
//
//            // Following https://stackoverflow.com/questions/36742888/spring-integration-java-dsl-configuration-of-aggregator
//            // Apparently, there is now scatterGather method which streamlines this flow
//            .publishSubscribeChannel {config -> config
//                    .subscribe{ c -> c
//                            .handle<LocalFile> { securedLocalFile, _ -> statsLogger.logStatsForSecuredFile(securedLocalFile) }
//                            .nullChannel()
//                    }
//                    .subscribe{ c -> c
//                            .handle<LocalFile> { securedLocalFile, _ -> janitor.cleanupSecuredFile(securedLocalFile) }
//                            .nullChannel()
//                    }
//            }
//            .get()

    @Bean
    internal fun executorChannel(): ExecutorChannelSpec =
            MessageChannels.executor(Executors.newFixedThreadPool(config.upload().maxConcurrentUploads()))

    @Bean
    internal fun securedFilesChannel(): QueueChannelSpec = MessageChannels.queue(SECURED_FILES_CHANNEL)

    @Bean
    internal fun securedFilesBatchedChannel(): PublishSubscribeChannelSpec<*>? = MessageChannels.publishSubscribe(SECURED_FILES_BATCHED_CHANNEL)

    @Bean
    internal fun statsLogger(): StatsLogger = StatsLogger()

    @MessagingGateway
    interface EventBusGateway : EventBus {

        @Gateway(requestChannel = REMOTE_DAILY_FOLDER_CREATED_CHANNEL)
        override fun notifyNewRemoteDailyFolderCreated(repoFolderName: RepoFolderName)

        @Gateway(requestChannel = SECURED_FILES_CHANNEL)
        override fun notifyFileSecured(localFile: LocalFile)
    }

    @Bean
    internal fun securerService(
            remoteRepository: RemoteRepository,
            @Suppress("SpringJavaInjectionPointsAutowiringInspection") eventBus: EventBus
    ): Securer = Securer(remoteRepository, eventBus)

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
    internal fun janitor(remoteRepository: RemoteRepository, config: UploaderConfigProperties): Janitor =
            Janitor(remoteRepository, config.rotation().maxDailyFolders())

    @Bean(name = [PollerMetadata.DEFAULT_POLLER])
    internal fun defaultPoller(): PollerMetadata =
            fixedDelay(POLLING_INTERVAL_IN_MILLIS.toLong(), TimeUnit.MILLISECONDS)
                    .maxMessagesPerPoll(BATCH_SIZE.toLong())
                    .get()

    @Bean
    internal fun pidWriter(): ApplicationListener<*> {
        val applicationPidFileWriter = ApplicationPidFileWriter()
        applicationPidFileWriter.setTriggerEventType(ApplicationReadyEvent::class.java)
        // applicationPidFileWriter.setTriggerEventType(ApplicationStartedEvent.class); // todo report?

        return applicationPidFileWriter
    }

    companion object {

        private val BATCH_SIZE = 4

        private const val SECURED_FILES_BATCHED_CHANNEL = "securedBatchedFilesChannel"
        private const val SECURED_FILES_CHANNEL = "securedFilesChannel"
        private const val REMOTE_DAILY_FOLDER_CREATED_CHANNEL = "remoteDailyFolderCreatedChannel"

        private val POLLING_INTERVAL_IN_MILLIS = 100
    }
}
