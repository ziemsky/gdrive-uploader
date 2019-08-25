package com.ziemsky.uploader.securing.infrastructure.googledrive

import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import com.ziemsky.uploader.securing.RemoteRepository
import com.ziemsky.uploader.securing.model.local.LocalFile
import com.ziemsky.uploader.securing.model.remote.RemoteFolder
import com.ziemsky.uploader.securing.model.remote.RemoteFolderName
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

private const val GOOGLE_DRIVE_FOLDER_MIMETYPE = "application/vnd.google-apps.folder"
private val DAILY_FOLDER_NAME_REGEX = """\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])""".toRegex()

class GDriveRemoteRepository(val drive: Drive) : RemoteRepository {

    // todo synchronised access?
    // todo Cache class?
    private val dailyFolders: MutableList<GDriveFolder> = mutableListOf()

    fun init() {
        // todo exception when any methods calls before init? delegate to external cache; possibly init on demand
        dailyFolders.clear()
        dailyFolders.addAll(getTopLevelFolders())

        log.debug("Initialised; remote folders found: $dailyFolders")
    }

    override fun dailyFolderCount(): Int = dailyFolders.size

    override fun findOldestDailyFolder(): RemoteFolder? = if (dailyFolders.isEmpty()) null else {
        // todo is the RemoteFolder/GDriveFolder actually useful? should the latter extend the former, instead?
        RemoteFolder.from(dailyFolders
                .asSequence()
                .sortedBy(GDriveFolder::name)
                .first()
                .name
        )
    }

    override fun deleteFolder(remoteFolder: RemoteFolder) {
        dailyFolders
                .find { it.name == remoteFolder.name.toString() }
                ?.let {
                    drive.files().delete(it.id).execute()
                    dailyFolders.removeIf {it.name == remoteFolder.name.toString()}
                }
    }

    override fun upload(targetFolder: RemoteFolder, localFile: LocalFile) {

        // Note: has to upload one by one because, at the moment of writing, Google Drive API doesn't support batching
        // of media uploads nor downloads, even though it does support batching other types of requests (applies to both
        // v2 and v3 version of he API).
        // See https://developers.google.com/drive/api/v3/batch
        // See https://developers.google.com/drive/api/v2/batch

        if (topLevelFolderWithNameAbsent(targetFolder.name)) {
            throw IllegalArgumentException("Target folder ${targetFolder.name} does not exist")
        }

        log.info("UPLOADING ${localFile.path} into ${findFolderByName(targetFolder.name)}")

        val gDriveFile = com.google.api.services.drive.model.File()
        gDriveFile.name = localFile.nameLocal.raw
        gDriveFile.parents = listOf(findFolderByName(targetFolder.name)?.id)

        val mediaContent = FileContent(null, localFile.file)

        drive.files().create(gDriveFile, mediaContent).execute()

        // todo media type
        // is metadata needed or will the client discover and populate it itself?")
        // val content: FileContent = FileContent("image/jpeg", localFile)
        // drive.files().create(gDriveFile, content)
    }

    override fun topLevelFolderWithNameAbsent(folderName: RemoteFolderName): Boolean {
        return findFolderByName(folderName) == null
    }

    override fun createFolderWithName(remoteFolderName: RemoteFolderName) {
        log.debug("Folder $remoteFolderName not found; creating.")

        val dir = com.google.api.services.drive.model.File()
        dir.name = remoteFolderName.toString()
        dir.mimeType = GOOGLE_DRIVE_FOLDER_MIMETYPE

        val newFolderId = drive.files().create(dir).execute().id

        log.debug("Folder $remoteFolderName not found; created with id $newFolderId.")

        cacheNewFolder(GDriveFolder(remoteFolderName.toString(), newFolderId))
    }

    private fun cacheNewFolder(gDriveFolder: GDriveFolder) {
        dailyFolders.add(gDriveFolder)
    }

    private fun findFolderByName(folderName: RemoteFolderName): GDriveFolder? {
        return dailyFolders.find { gDriveFolder -> gDriveFolder.name == folderName.toString() }
    }


    private fun getTopLevelFolders(): MutableList<GDriveFolder> = drive
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
            ?.toMutableList() ?: mutableListOf()
}

data class GDriveFolder(val name: String, val id: String)