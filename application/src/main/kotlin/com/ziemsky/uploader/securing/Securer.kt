package com.ziemsky.uploader.securing

import com.ziemsky.uploader.securing.model.SecuredFileSummary
import com.ziemsky.uploader.securing.model.local.LocalFile
import com.ziemsky.uploader.securing.model.remote.RemoteFolder
import com.ziemsky.uploader.securing.model.remote.RemoteFolderName
import mu.KotlinLogging
import java.time.Clock
import java.time.Instant.now

private val log = KotlinLogging.logger {}

class Securer(
        private val remoteRepository: RemoteRepository,
        private val domainEventsNotifier: DomainEventsNotifier,
        private val clock: Clock
) {

    fun secure(localFile: LocalFile) {

        log.debug { "Securing $localFile" }

        val dailyRepoFolder = RemoteFolder.from(localFile.date)

        val instantStart = now(clock)

        remoteRepository.upload(dailyRepoFolder, localFile)

        val instantStop = now(clock)

        domainEventsNotifier.notifyFileSecured(SecuredFileSummary(instantStart, instantStop, localFile))
    }


    fun ensureRemoteDailyFolder(localFile: LocalFile) {

        val dailyRepoFolderName = RemoteFolderName.from(localFile.date)

        if (remoteRepository.topLevelFolderWithNameAbsent(dailyRepoFolderName)) {
            remoteRepository.createTopLevelFolder(dailyRepoFolderName)
            log.debug { "Created folder $dailyRepoFolderName" }
            domainEventsNotifier.notifyNewRemoteDailyFolderCreated(dailyRepoFolderName)
        }

    }
}