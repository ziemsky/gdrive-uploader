package com.ziemsky.uploader

import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import com.ziemsky.uploader.model.local.LocalFile
import com.ziemsky.uploader.model.repo.RepoFolder
import com.ziemsky.uploader.model.repo.RepoFolderName
import mu.KotlinLogging

private val log = KotlinLogging.logger {}
private const val GOOGLE_DRIVE_FOLDER_MIMETYPE = "application/vnd.google-apps.folder"

class GDriveFileRepository(val drive: Drive) : FileRepository {

    private val topLevelFolders: MutableList<GDriveFolder> = getTopLevelFolders() // todo synchronised access?

    override fun dailyFolderCount(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun upload(targetFolder: RepoFolder, localFile: LocalFile) {

        // Note: has to upload one by one because, at the moment of writing, Google Drive API doesn't support batching
        // of media uploads nor downloads, even though it does support batching other types of requests.
        // See https://developers.google.com/drive/api/v3/batch

        // Consider switching to API v2 if performance (or request rate) proves to be an issue here - it supports batch
        // uploads

        if (folderWithNameAbsent(targetFolder.name)) {
            createFolderWithName(targetFolder.name)
        }

        log.info("UPLOADING ${localFile.path} into ${findFolderByName(targetFolder.name)}")

        val gDriveFile = com.google.api.services.drive.model.File()
        gDriveFile.name = localFile.name.raw
        gDriveFile.parents = listOf(findFolderByName(targetFolder.name)?.id)

        val mediaContent = FileContent(null, localFile.file)

        drive.files().create(gDriveFile, mediaContent).execute()

        // todo media type
        // is metadata needed or will the client discover and populate it itself?")
        // val content: FileContent = FileContent("image/jpeg", localFile)
        // drive.files().create(gDriveFile, content)
    }

    override fun findOldestDailyFolder(): RepoFolder {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deleteFolder(repoFolder: RepoFolder) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun folderWithNameAbsent(folderName: RepoFolderName): Boolean {
        return findFolderByName(folderName) == null
    }

    private fun createFolderWithName(repoFolderName: RepoFolderName) {
        log.debug("Folder $repoFolderName not found; creating.")

        val dir = com.google.api.services.drive.model.File()
        dir.name = repoFolderName.toString()
        dir.mimeType = GOOGLE_DRIVE_FOLDER_MIMETYPE

        val newFolderId = drive.files().create(dir).execute().id

        cacheNewFolder(GDriveFolder(repoFolderName.toString(), newFolderId))
    }

    private fun cacheNewFolder(gDriveFolder: GDriveFolder) {
        topLevelFolders.add(gDriveFolder)
    }

    private fun findFolderByName(folderName: RepoFolderName): GDriveFolder? {
        return topLevelFolders.find { gDriveFolder -> gDriveFolder.name == folderName.toString() }
    }

    private fun getTopLevelFolders(): MutableList<GDriveFolder> = drive
            .files()
            .list()
            .setSpaces("drive")
            .setQ("mimeType='$GOOGLE_DRIVE_FOLDER_MIMETYPE' and 'root' in parents")
            .execute()
            .files.map { file -> GDriveFolder(file.name, file.id) } // todo handle paging: https://developers.google.com/drive/api/v3/search-parameters
            .toMutableList()
}

data class GDriveFolder(val name: String, val id: String)