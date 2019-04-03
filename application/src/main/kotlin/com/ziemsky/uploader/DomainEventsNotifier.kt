package com.ziemsky.uploader

import com.ziemsky.uploader.model.repo.RepoFolderName

interface DomainEventsNotifier {

    fun notifyNewRemoteDailyFolderCreated(repoFolderName: RepoFolderName)

    fun notifyFileSecured(securedFileSummary: SecuredFileSummary)
}
