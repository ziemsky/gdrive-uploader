package com.ziemsky.uploader

import com.ziemsky.uploader.model.local.LocalFile
import com.ziemsky.uploader.model.repo.RepoFolder
import mu.KotlinLogging

private val log = KotlinLogging.logger {}
class Janitor(private val fileRepository: FileRepository, private val maxDailyFoldersCount: Int) {

    fun cleanupSecuredFile(localFile: LocalFile) {
        log.info { "Deleting $localFile" }

        localFile.file.delete()
    }

    fun rotateRemoteDailyFolders() {
        while (fileRepository.dailyFolderCount() > maxDailyFoldersCount) {
            val oldestRemoteFolder: RepoFolder = fileRepository.findOldestDailyFolder()

            fileRepository.deleteFolder(oldestRemoteFolder)
        }
    }
}
