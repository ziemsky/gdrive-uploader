package com.ziemsky.uploader.securing

import com.ziemsky.uploader.securing.model.SecuredFileSummary
import com.ziemsky.uploader.securing.model.local.LocalFile
import com.ziemsky.uploader.securing.model.remote.RemoteDailyFolder
import com.ziemsky.uploader.securing.model.remote.RemoteFolderName
import mu.KotlinLogging
import java.time.Clock
import java.time.Instant.now

private val log = KotlinLogging.logger {}

class Securer(
        private val remoteStorageService: RemoteStorageService,
        private val domainEventsNotifier: DomainEventsNotifier,
        private val clock: Clock
) {

    fun secure(localFile: LocalFile) {

        log.info { "Securing $localFile." }

        val dailyRepoFolder = RemoteDailyFolder.from(localFile.date())

        val instantStart = now(clock)

        remoteStorageService.upload(dailyRepoFolder, localFile)

        log.info { "Secured $localFile." }

        val instantStop = now(clock)

        domainEventsNotifier.notifyFileSecured(SecuredFileSummary(instantStart, instantStop, localFile))
    }


    fun ensureRemoteDailyFolder(localFile: LocalFile) {

        val dailyRepoFolderName = RemoteFolderName.from(localFile.date())

        if (remoteStorageService.isTopLevelFolderWithNameAbsent(dailyRepoFolderName)) {
            log.info { "Creating folder $dailyRepoFolderName." }
            remoteStorageService.createTopLevelFolder(dailyRepoFolderName)
            log.info { "Created folder $dailyRepoFolderName." }

            domainEventsNotifier.notifyNewRemoteDailyFolderCreated(dailyRepoFolderName)
        }
    }


}