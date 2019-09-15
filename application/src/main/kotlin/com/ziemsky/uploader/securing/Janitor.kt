package com.ziemsky.uploader.securing

import com.ziemsky.uploader.securing.model.local.LocalFile
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

class Janitor(private val remoteRepository: RemoteRepository, private val maxDailyFoldersCount: Int) {

    fun cleanupSecuredFile(localFile: LocalFile) {
        log.info { "Deleting $localFile" }

        localFile.file.delete()
    }

    fun rotateRemoteDailyFolders() {
        while (remoteRepository.dailyFolderCount() > maxDailyFoldersCount) {
            remoteRepository.findOldestDailyFolder()?.let(remoteRepository::deleteDailyFolder)
        }
    }
}