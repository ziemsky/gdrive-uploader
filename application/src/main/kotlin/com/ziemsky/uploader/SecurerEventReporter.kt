package com.ziemsky.uploader

import com.ziemsky.uploader.model.repo.RepoFolderName

interface SecurerEventReporter {

    fun notifyNewRemoteDailyFolderCreated(repoFolderName: RepoFolderName)

}
