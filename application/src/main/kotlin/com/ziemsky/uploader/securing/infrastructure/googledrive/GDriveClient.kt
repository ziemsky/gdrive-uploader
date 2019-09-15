package com.ziemsky.uploader.securing.infrastructure.googledrive

import com.google.api.client.http.FileContent
import com.google.api.services.drive.model.File
import com.ziemsky.uploader.securing.infrastructure.googledrive.model.GDriveFolder

interface GDriveClient {
    fun getTopLevelDailyFolders(): List<GDriveFolder>

    fun upload(gDriveFile: File, mediaContent: FileContent)

    fun deleteFolder(remoteFolder: GDriveFolder)

    fun createTopLevelFolder(folderName: String): GDriveFolder
}