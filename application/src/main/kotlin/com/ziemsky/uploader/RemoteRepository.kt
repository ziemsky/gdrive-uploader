package com.ziemsky.uploader

import com.ziemsky.uploader.model.local.LocalFile
import com.ziemsky.uploader.model.repo.RepoFolder
import com.ziemsky.uploader.model.repo.RepoFolderName

interface RemoteRepository { // todo rename

    fun upload(targetFolder: RepoFolder, localFile: LocalFile)

    fun dailyFolderCount(): Int

    fun findOldestDailyFolder(): RepoFolder?

    fun deleteFolder(repoFolder: RepoFolder)

    fun topLevelFolderWithNameAbsent(folderName: RepoFolderName): Boolean

    fun createFolderWithName(repoFolderName: RepoFolderName)
}
