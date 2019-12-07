package com.ziemsky.uploader.securing.infrastructure.googledrive

import com.google.api.client.http.FileContent
import com.ziemsky.uploader.securing.RemoteStorageService
import com.ziemsky.uploader.securing.infrastructure.googledrive.model.GDriveFolder
import com.ziemsky.uploader.securing.model.local.LocalFile
import com.ziemsky.uploader.securing.model.remote.RemoteDailyFolder
import com.ziemsky.uploader.securing.model.remote.RemoteFolderName
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

class GDriveRemoteStorageService(val gDriveClient: GDriveClient, val rootFolderName: String) : RemoteStorageService {

    // todo synchronised access!
    // todo Cache class? @Cacheable annotation?
    // low priority as the usage pattern is such that concurrent access errors (due to writing) are extremely unlikely,
    // as this property is populated once on application start, from a single thread.
    private val dailyFolders: MutableList<GDriveFolder> = mutableListOf()

    private lateinit var rootFolder: GDriveFolder

    fun init() {
        // todo exception when any methods calls before init? delegate to external cache; possibly init on demand
        rootFolder = gDriveClient.getRootFolder(rootFolderName)

        initDailyFoldersCache()

        log.debug { "Initialised." }
    }

    override fun dailyFolderCount(): Int = dailyFolders.size

    override fun findOldestDailyFolder(): RemoteDailyFolder? = if (dailyFolders.isEmpty()) null else {
        // todo is the RemoteFolder/GDriveFolder actually useful? should the latter extend the former, instead?
        RemoteDailyFolder.from(dailyFolders
                .asSequence()
                .sortedBy(GDriveFolder::name)
                .first()
                .name
        )
    }

    override fun deleteDailyFolder(remoteDailyFolder: RemoteDailyFolder) {
        log.debug { "Deleting daily folder $remoteDailyFolder" }

        dailyFolders
                .find { it.name == remoteDailyFolder.name.toString() }
                ?.let {
                    gDriveClient.deleteFolder(it)
                    dailyFolders.removeIf {it.name == remoteDailyFolder.name.toString()}
                }

        logDailyFoldersCache()
    }

    override fun upload(targetDailyFolder: RemoteDailyFolder, localFile: LocalFile) {

        // Note: has to upload one by one because, at the moment of writing, Google Drive API doesn't support batching
        // of media uploads nor downloads, even though it does support batching other types of requests (applies to both
        // v2 and v3 version of he API).
        // See https://developers.google.com/drive/api/v3/batch
        // See https://developers.google.com/drive/api/v2/batch

        if (isTopLevelFolderWithNameAbsent(targetDailyFolder.name)) {
            // We deliberately do not create a folder here, but expect that it has already been created.
            // This is because this method will be called from different threads in parallel and we could easily
            // end up in the situation where the same folder is attempted to be created twice.
            throw IllegalArgumentException("Failed to upload; target folder ${targetDailyFolder.name} does not exist")
        }

        log.debug { "Uploading ${localFile.path()} into ${findFolderByName(targetDailyFolder.name)}" }

        val gDriveFile = com.google.api.services.drive.model.File()
        gDriveFile.name = localFile.nameLocal().raw
        gDriveFile.parents = listOf(findFolderByName(targetDailyFolder.name)?.id)


        // todo get configurable parent folder here from locally cached value;
        // cache it in init() so that it is only retrieved once on app start


        val mediaContent = FileContent(null, localFile.raw())

        gDriveClient.upload(gDriveFile, mediaContent)

        log.debug { "Uploaded ${localFile.path()} into ${findFolderByName(targetDailyFolder.name)}" }

        // todo media type
        // is metadata needed or will the client discover and populate it itself?")
        // val content: FileContent = FileContent("image/jpeg", localFile)
        // drive.files().create(gDriveFile, content)
    }

    override fun isTopLevelFolderWithNameAbsent(folderName: RemoteFolderName): Boolean {
        return findFolderByName(folderName) == null
    }

    override fun createTopLevelFolder(remoteFolderName: RemoteFolderName) {
        log.debug { "Creating top level folder $remoteFolderName" }

        val createdFolder = gDriveClient.createTopLevelFolder(rootFolder.id, remoteFolderName.toString())

        log.debug { "Created top level folder $createdFolder" }

        cacheNewFolder(createdFolder)
    }

    private fun initDailyFoldersCache() {
        dailyFolders.clear()
        dailyFolders.addAll(gDriveClient.childFoldersOf(rootFolder))

        logDailyFoldersCache()
    }

    private fun cacheNewFolder(gDriveFolder: GDriveFolder) {
        dailyFolders.add(gDriveFolder)
        logDailyFoldersCache()
    }

    private fun logDailyFoldersCache() {
        log.debug { "Daily folders cached [${dailyFolders.size}]: $dailyFolders" }
    }

    private fun findFolderByName(folderName: RemoteFolderName): GDriveFolder? {
        return dailyFolders.find { gDriveFolder -> gDriveFolder.name == folderName.toString() }
    }
}