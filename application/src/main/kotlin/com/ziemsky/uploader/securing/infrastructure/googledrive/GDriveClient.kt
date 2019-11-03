package com.ziemsky.uploader.securing.infrastructure.googledrive

import com.google.api.client.http.FileContent
import com.google.api.services.drive.model.File
import com.ziemsky.uploader.securing.infrastructure.googledrive.model.GDriveFolder

interface GDriveClient {

    // todo reflect the fact that it's only matching folders that are retrieved
    // provide matching regex as param, passed in from the service? Clients shouldn't have concept of 'daily folders'
    fun childFoldersOf(rootFolder: GDriveFolder): List<GDriveFolder>

    fun upload(gDriveFile: File, mediaContent: FileContent)

    fun deleteFolder(remoteFolder: GDriveFolder)

    fun createTopLevelFolder(rootFolderId: String, folderName: String): GDriveFolder

    fun getRootFolder(rootFolderName: String): GDriveFolder
}