package com.ziemsky.uploader

import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import com.ziemsky.uploader.model.local.LocalFile
import com.ziemsky.uploader.model.repo.RepoFolder
import com.ziemsky.uploader.model.repo.RepoFolderName
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

    override fun findOldestDailyFolder(): RepoFolder? = if (dailyFolders.isEmpty()) null else {
        // todo is the RemoteFolder/GDriveFolder actually useful? should the latter extend the former, instead?
        RepoFolder.from(dailyFolders
                .asSequence()
                .sortedBy(GDriveFolder::name)
                .first()
                .name
        )
    }

    override fun deleteFolder(repoFolder: RepoFolder) {
        dailyFolders
                .find { it.name == repoFolder.name.toString() }
                ?.let {
                    drive.files().delete(it.id).execute()
                    dailyFolders.removeIf {it.name == repoFolder.name.toString()}
                }
    }

    override fun upload(targetFolder: RepoFolder, localFile: LocalFile) {

        // Note: has to upload one by one because, at the moment of writing, Google Drive API doesn't support batching
        // of media uploads nor downloads, even though it does support batching other types of requests.
        // See https://developers.google.com/drive/api/v3/batch

        // Consider switching to API v2 if performance (or request rate) proves to be an issue here - it supports batch
        // uploads

        if (topLevelFolderWithNameAbsent(targetFolder.name)) {
            throw IllegalArgumentException("Target folder ${targetFolder.name} does not exist")
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

    override fun topLevelFolderWithNameAbsent(folderName: RepoFolderName): Boolean {
        return findFolderByName(folderName) == null
    }

    override fun createFolderWithName(repoFolderName: RepoFolderName) {
        log.debug("Folder $repoFolderName not found; creating.")

        val dir = com.google.api.services.drive.model.File()
        dir.name = repoFolderName.toString()
        dir.mimeType = GOOGLE_DRIVE_FOLDER_MIMETYPE

        val newFolderId = drive.files().create(dir).execute().id

        log.debug("Folder $repoFolderName not found; created with id $newFolderId.")

        cacheNewFolder(GDriveFolder(repoFolderName.toString(), newFolderId))
    }

    private fun cacheNewFolder(gDriveFolder: GDriveFolder) {
        dailyFolders.add(gDriveFolder)
    }

    private fun findFolderByName(folderName: RepoFolderName): GDriveFolder? {
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