package com.ziemsky.uploader.application.conf

import com.google.api.services.drive.Drive
import com.ziemsky.uploader.securing.DomainEventsNotifier
import com.ziemsky.uploader.securing.Janitor
import com.ziemsky.uploader.securing.RemoteStorageService
import com.ziemsky.uploader.securing.Securer
import com.ziemsky.uploader.securing.infrastructure.googledrive.*
import com.ziemsky.uploader.securing.model.SecuredFileSummary
import com.ziemsky.uploader.securing.model.local.LocalFile
import com.ziemsky.uploader.securing.model.remote.RemoteFolderName
import com.ziemsky.uploader.stats.StatsCalculator
import com.ziemsky.uploader.stats.reporting.StatsReporter
import com.ziemsky.uploader.stats.reporting.logging.HumanReadableStatsLogsRenderer
import com.ziemsky.uploader.stats.reporting.logging.LoggingStatsReporter
import com.ziemsky.uploader.stats.reporting.logging.infrastructure.Slf4jStatsLogger
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
import java.time.Clock
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
    //    domainEventsNotifier.notifyFileSecured(LocalFile)
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
        log.info("                  application version: {}", env.getProperty("info.app.version"))
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
                                .regexFilter(config.upload().fileNameRegex())
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
            .handle<RemoteFolderName> { _, _ -> janitor.rotateRemoteDailyFolders() }
            .nullChannel()

    @Bean
    internal fun filesBatchAggregator(statsReporter: StatsReporter, janitor: Janitor): IntegrationFlow = IntegrationFlows
            .from(SECURED_FILES_CHANNEL)

            // Following https://stackoverflow.com/questions/36742888/spring-integration-java-dsl-configuration-of-aggregator
            // Apparently, there is now scatterGather method which streamlines this flow
            .publishSubscribeChannel {config -> config
                    .subscribe { c ->
                        c.aggregate { aggregatorSpec ->
                            aggregatorSpec
                                    .correlationStrategy { message -> true }
                                    .releaseStrategy { group -> group.size() == STATS_BATCH_SIZE }
                                    .groupTimeout(10000)
                                    .sendPartialResultOnExpiry(true)
                                    .expireGroupsUponCompletion(true)
                        }
                                .transform(List::class.java) { list -> list.toHashSet() }
                                .handle<Set<SecuredFileSummary>> { securedLocalFiles, _ -> statsReporter.reportStatsForSecuredFiles(securedLocalFiles) }
                                .nullChannel()
                    }
                    .subscribe { c -> c
                            .handle<SecuredFileSummary> { securedLocalFile, _ -> janitor.cleanupSecuredFile(securedLocalFile.securedFile) }
                            .nullChannel()
                    }
            }
            .get()

    @Bean
    internal fun executorChannel(): ExecutorChannelSpec =
            MessageChannels.executor(Executors.newFixedThreadPool(config.upload().maxConcurrentUploads()))

    @Bean
    internal fun securedFilesChannel(): QueueChannelSpec = MessageChannels.queue(SECURED_FILES_CHANNEL)

    @Bean
    internal fun statsReporter(): LoggingStatsReporter = LoggingStatsReporter(StatsCalculator(), Slf4jStatsLogger(HumanReadableStatsLogsRenderer()))

    @MessagingGateway
    interface DomainEventsNotifierSpringIntegrationGateway : DomainEventsNotifier { // todo move out of here

        @Gateway(requestChannel = REMOTE_DAILY_FOLDER_CREATED_CHANNEL)
        override fun notifyNewRemoteDailyFolderCreated(remoteFolderName: RemoteFolderName)

        @Gateway(requestChannel = SECURED_FILES_CHANNEL)
        override fun notifyFileSecured(securedFileSummary: SecuredFileSummary)
    }

    @Bean
    internal fun securerService(
            remoteStorageService: RemoteStorageService,
            @Suppress("SpringJavaInjectionPointsAutowiringInspection") domainEventsNotifier: DomainEventsNotifier,
            clock: Clock
    ): Securer = Securer(remoteStorageService, domainEventsNotifier, clock)

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
    internal fun gDriveClient(drive: Drive, config: UploaderConfigProperties): GDriveClient =
            GDriveRetryingClient(
                    GDriveDirectClient(drive),
                    config.upload().retryTimeout()
            )

    @Bean
    internal fun remoteStorageService(drive: Drive, gDriveClient: GDriveClient, config: UploaderConfigProperties): RemoteStorageService {

        val gDriveRemoteStorageService = GDriveRemoteStorageService(gDriveClient, config.upload().rootFolderName())

        gDriveRemoteStorageService.init()

        return gDriveRemoteStorageService
    }

    @Bean
    internal fun janitor(remoteStorageService: RemoteStorageService, config: UploaderConfigProperties): Janitor =
            Janitor(remoteStorageService, config.rotation().maxDailyFolders())

    @Bean(name = [PollerMetadata.DEFAULT_POLLER])
    internal fun defaultPoller(): PollerMetadata =
            fixedDelay(POLLING_INTERVAL_IN_MILLIS.toLong(), TimeUnit.MILLISECONDS)
                    .maxMessagesPerPoll(BATCH_SIZE.toLong())
                    .get()

    @Bean
    internal fun clock() = Clock.systemDefaultZone()

    @Bean
    internal fun pidWriter(): ApplicationListener<*> {
        val applicationPidFileWriter = ApplicationPidFileWriter()
        applicationPidFileWriter.setTriggerEventType(ApplicationReadyEvent::class.java)
        // applicationPidFileWriter.setTriggerEventType(ApplicationStartedEvent.class); // todo report?

        return applicationPidFileWriter
    }

    companion object {

        private val BATCH_SIZE = 10
        private val STATS_BATCH_SIZE = 10

        private const val SECURED_FILES_CHANNEL = "securedFilesChannel"
        private const val REMOTE_DAILY_FOLDER_CREATED_CHANNEL = "remoteDailyFolderCreatedChannel"

        private val POLLING_INTERVAL_IN_MILLIS = 1_000
    }
}
