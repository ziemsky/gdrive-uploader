package com.ziemsky.uploader.securing.infrastructure.googledrive

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
import com.google.api.services.drive.model.File
import com.ziemsky.uploader.securing.infrastructure.BlockingRetryingExecutor.Companion.retryOnException
import com.ziemsky.uploader.securing.infrastructure.googledrive.model.GDriveFolder
import java.net.SocketTimeoutException
import java.time.Duration

class GDriveRetryingClient(private val gDriveDirectClient: GDriveDirectClient, private val retryTimeout: Duration) : GDriveClient {

    override fun getRootFolder(rootFolderName: String): GDriveFolder {

        lateinit var rootFolder: GDriveFolder

        retryOnUsageLimitsException { rootFolder = gDriveDirectClient.getRootFolder(rootFolderName) }

        return rootFolder
    }

    override fun upload(gDriveFile: File, mediaContent: FileContent) = retryOnUsageLimitsException {
        gDriveDirectClient.upload(gDriveFile, mediaContent)
    }

    override fun childFoldersOf(parentFolder: GDriveFolder): List<GDriveFolder> {

        lateinit var topLevelDailyFolders: List<GDriveFolder>

        retryOnUsageLimitsException { topLevelDailyFolders = gDriveDirectClient.childFoldersOf(parentFolder) }

        return topLevelDailyFolders
    }

    override fun deleteFolder(remoteFolder: GDriveFolder) = retryOnUsageLimitsException {
        gDriveDirectClient.deleteFolder(remoteFolder)
    }

    override fun createTopLevelFolder(rootFolderId: String, folderName: String): GDriveFolder {

        lateinit var newlyCreatedTopLevelFolder: GDriveFolder

        retryOnUsageLimitsException { newlyCreatedTopLevelFolder = gDriveDirectClient.createTopLevelFolder(rootFolderId, folderName) }

        return newlyCreatedTopLevelFolder
    }

    private fun retryOnUsageLimitsException(action: () -> Unit) {

        return retryOnException(
                retryableExceptionPredicate = { throwable ->
                    throwable is GoogleJsonResponseException
                            && throwable.statusCode == 403
                            && with(throwable.details.errors) {
                        isNotEmpty()
                                && this[0].domain == "usageLimits"
                                && this[0].reason == "userRateLimitExceeded"
                    }
                            || throwable is SocketTimeoutException
                },
                timeout = retryTimeout,
                actionOnExpiration = { throw TimeoutException("Giving up on retrying; timeout expired: $retryTimeout") },
                action = action
        )
    }
}