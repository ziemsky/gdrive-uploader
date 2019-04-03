package com.ziemsky.uploader

import com.ziemsky.uploader.model.local.LocalFile
import com.ziemsky.uploader.model.repo.RepoFolderName

interface EventBus {

    fun notifyNewRemoteDailyFolderCreated(repoFolderName: RepoFolderName)

    fun notifyFileSecured(localFile: LocalFile)
}
