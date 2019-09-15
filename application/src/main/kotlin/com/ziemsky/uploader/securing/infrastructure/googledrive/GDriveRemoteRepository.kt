package com.ziemsky.uploader.securing.infrastructure.googledrive

import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import com.ziemsky.uploader.securing.RemoteRepository
import com.ziemsky.uploader.securing.infrastructure.googledrive.model.GDriveFolder
import com.ziemsky.uploader.securing.model.local.LocalFile
import com.ziemsky.uploader.securing.model.remote.RemoteFolder
import com.ziemsky.uploader.securing.model.remote.RemoteFolderName
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

private const val GOOGLE_DRIVE_FOLDER_MIMETYPE = "application/vnd.google-apps.folder"

class GDriveRemoteRepository(val drive: Drive, val gDriveClient: GDriveClient) : RemoteRepository {

    // todo synchronised access? immutable?
    // todo Cache class? @Cacheable annotation?
    // low priority as the usage pattern is such that concurrent access errors are very unlikely
    private val dailyFolders: MutableList<GDriveFolder> = mutableListOf()

    fun init() {
        // todo exception when any methods calls before init? delegate to external cache; possibly init on demand
        dailyFolders.clear()
        dailyFolders.addAll(gDriveClient.getTopLevelDailyFolders())

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

    override fun deleteDailyFolder(remoteFolder: RemoteFolder) {
        dailyFolders
                .find { it.name == remoteFolder.name.toString() }
                ?.let {
                    gDriveClient.deleteFolder(it)
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

        gDriveClient.upload(gDriveFile, mediaContent)

        // todo media type
        // is metadata needed or will the client discover and populate it itself?")
        // val content: FileContent = FileContent("image/jpeg", localFile)
        // drive.files().create(gDriveFile, content)
    }

    override fun topLevelFolderWithNameAbsent(folderName: RemoteFolderName): Boolean {
        return findFolderByName(folderName) == null
    }

    override fun createTopLevelFolder(remoteFolderName: RemoteFolderName) {
        log.debug("Creating top level folder $remoteFolderName")

        val createdFolder = gDriveClient.createTopLevelFolder(remoteFolderName.toString())

        log.debug("Created top level folder $createdFolder")

        cacheNewFolder(createdFolder)
    }

    private fun cacheNewFolder(gDriveFolder: GDriveFolder) {
        dailyFolders.add(gDriveFolder)
    }

    private fun findFolderByName(folderName: RemoteFolderName): GDriveFolder? {
        return dailyFolders.find { gDriveFolder -> gDriveFolder.name == folderName.toString() }
    }
}