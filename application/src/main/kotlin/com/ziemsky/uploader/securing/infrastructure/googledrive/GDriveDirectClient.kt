package com.ziemsky.uploader.securing.infrastructure.googledrive

import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.ziemsky.uploader.securing.infrastructure.googledrive.model.GDriveFolder

private const val GOOGLE_DRIVE_FOLDER_MIMETYPE = "application/vnd.google-apps.folder"
private val DAILY_FOLDER_NAME_REGEX = """\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])""".toRegex()

class GDriveDirectClient(private val drive: Drive) : GDriveClient {

    override fun upload(gDriveFile: File, mediaContent: FileContent) {
        drive.files().create(gDriveFile, mediaContent).execute()
    }

    override fun getTopLevelDailyFolders(): List<GDriveFolder> = drive
            // https://developers.google.com/drive/api/v3/search-parameters
            .files()
            .list()
            .setSpaces("drive")
            .setQ("mimeType='$GOOGLE_DRIVE_FOLDER_MIMETYPE' and 'root' in parents")
            .execute()
            .files
            ?.asSequence()
            ?.filter { file -> DAILY_FOLDER_NAME_REGEX.matches(file.name) }
            ?.map { file -> GDriveFolder(file.name, file.id) } // todo handle paging: https://developers.google.com/drive/api/v3/search-parameters
            ?.toList() ?: listOf()

    override fun deleteFolder(remoteFolder: GDriveFolder) {
        drive.files().delete(remoteFolder.id).execute()
    }

    override fun createTopLevelFolder(folderName: String): GDriveFolder {
        val dir = File()
        dir.name = folderName
        dir.mimeType = GOOGLE_DRIVE_FOLDER_MIMETYPE

        val folderId = drive.files().create(dir).execute().id

        return GDriveFolder(folderName, folderId)
    }
}