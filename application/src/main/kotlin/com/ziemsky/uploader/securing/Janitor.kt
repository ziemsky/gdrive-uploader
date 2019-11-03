package com.ziemsky.uploader.securing

import com.ziemsky.uploader.securing.model.local.LocalFile
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

class Janitor(private val remoteStorageService: RemoteStorageService, private val maxDailyFoldersCount: Int) {

    fun cleanupSecuredFile(localFile: LocalFile) {
        log.info { "Deleting $localFile." }
        localFile.file.delete()
        log.info { "Deleted $localFile." }
    }

    fun rotateRemoteDailyFolders() {
        while (remoteStorageService.dailyFolderCount() > maxDailyFoldersCount) {
            remoteStorageService.findOldestDailyFolder()?.let {
                log.info { "Deleting remote folder $it." }
                remoteStorageService.deleteDailyFolder(it)
                log.info { "Deleted remote folder $it." }
            }
        }
    }
}