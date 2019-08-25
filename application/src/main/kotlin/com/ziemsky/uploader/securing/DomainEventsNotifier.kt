package com.ziemsky.uploader.securing

import com.ziemsky.uploader.securing.model.SecuredFileSummary
import com.ziemsky.uploader.securing.model.remote.RemoteFolderName

interface DomainEventsNotifier {

    fun notifyNewRemoteDailyFolderCreated(remoteFolderName: RemoteFolderName)

    fun notifyFileSecured(securedFileSummary: SecuredFileSummary)
}
