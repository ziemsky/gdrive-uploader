package com.ziemsky.uploader.test.shared.data

import com.google.api.client.googleapis.batch.json.JsonBatchCallback
import com.google.api.client.googleapis.json.GoogleJsonError
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.HttpHeaders
import com.google.api.services.drive.Drive
import com.ziemsky.fsstructure.FsDir
import com.ziemsky.fsstructure.FsItem
import com.ziemsky.fsstructure.FsStructure
import com.ziemsky.uploader.test.shared.RetryingExecutor
import mu.KotlinLogging
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

private val log = KotlinLogging.logger {}
private val GOOGLE_DRIVE_FOLDER_MIMETYPE = "application/vnd.google-apps.folder"

class TestFixtures( // todo make local fixtures handled separately from remote?
        private val testDirectory: Path,
        private val drive: Drive
) {

    fun clearTempDir() {
        testDirectory.toFile().list().forEach { fileName ->
            println("file to delete: ${Paths.get(testDirectory.toAbsolutePath().toString(), fileName)}")
        }
    }

    fun createLocalTestFilesToSecure(totalFilesCount: Int, fileSizeInBytes: Int) {
        // example file name: 20180909120000-02-front.jpg
        (1..totalFilesCount)
                .forEach { fileOrdinal ->
                    val formattedTimeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))

                    val fileName = "$formattedTimeStamp-$fileOrdinal-front.jpg"

                    createLocalFileWithRandomContent(fileName, fileSizeInBytes)

                    println("created local file [$fileOrdinal]: $fileName (size in bytes: $fileSizeInBytes)")
                }
    }


    private fun createLocalFileWithRandomContent(name: String, lengthInBytes: Int): File {
        val createdFile = Files.createFile(Paths.get(testDirectory.toString(), name)).toFile()

        createdFile.writeBytes(randomBytes(lengthInBytes))

        return createdFile
    }

    private fun randomBytes(count: Int) = Random.nextBytes(ByteArray(count))

    fun remoteStructure(): FsStructure {
        val root = retryOnUsageLimitsException { drive.files().get("root").setFields("id").execute() }

        val fullUndeletedGDriveContent = liveFilesListQuery().setFields("files(id, name, mimeType, parents)").execute()

        val fsItems: List<FsItem> = children(root.id, fullUndeletedGDriveContent.files)

        val fsStructure = FsStructure.create(fsItems)

        return fsStructure
    }

    fun remoteStructureDelete() {

        val fullGDriveContent = retryOnUsageLimitsException {drive.files().list().setFields("files(id, name, mimeType)").execute()}

        log.info("Deleting {} remote files", fullGDriveContent.files.size)

        if (fullGDriveContent.files.isEmpty()) return

        val deletionBatch = drive.batch()

        fullGDriveContent.files.forEach {
            drive.files()
                    .delete(it.id)
                    .queue(
                            deletionBatch,
                            object : JsonBatchCallback<Void>() {

                                fun itemType(): String {
                                    return if (GOOGLE_DRIVE_FOLDER_MIMETYPE.equals(it.mimeType)) "folder" else "file"
                                }

                                override fun onSuccess(file: Void?, responseHeaders: HttpHeaders) {
                                    log.info("Deleted {} {}: '{}'", itemType(), it.id, it.name)
                                }

                                override fun onFailure(e: GoogleJsonError, responseHeaders: HttpHeaders) {
                                    log.error("Failed to delete ${itemType()} ${it.id}: '${it.name}'", e)
                                }
                            }
                    )
        }

        retryOnUsageLimitsException { deletionBatch.execute() }
    }

    fun remoteStructureCreateFrom(remoteContent: FsStructure?) {

        val parents = HashMap<FsDir, com.google.api.services.drive.model.File>()

        remoteContent?.walk(
                { dirItem ->
                    var dir = com.google.api.services.drive.model.File()
                    dir.setName(dirItem.name())
                    dir.setMimeType(GOOGLE_DRIVE_FOLDER_MIMETYPE)

                    if (dirItem.isNested()) {
                        val parentId: String = parents[dirItem.parent()]?.id ?: "root"
                        dir.setParents(listOf(parentId))
                    }

                    dir = retryOnUsageLimitsException { drive.files().create(dir).execute() }

                    log.info { "Created remote ${dirItem} with id ${dir.id}" }

                    parents.put(dirItem, dir)
                },
                { fileItem ->
                    val file = com.google.api.services.drive.model.File()
                    file.setName(fileItem.name())

                    if (fileItem.isNested()) {
                        val parentId: String? = parents[fileItem.parent()]?.id
                        file.setParents(listOf(parentId))
                    }

                    val mediaContent = ByteArrayContent(null, fileItem?.content)

                    val fileId = retryOnUsageLimitsException { drive.files().create(file, mediaContent).execute().id }

                    log.info { "Created remote ${fileItem} with id ${fileId}" }
                }
        )
    }

    fun localStructureCreateFrom(localContent: FsStructure?) {
        val tempDirectory = Files.createTempDirectory("tmp_")

        localContent?.saveIn(tempDirectory)

        moveDirContent(tempDirectory, testDirectory)

        tempDirectory.toFile().delete()
    }

    fun localMonitoredDirEmpty(): Boolean {
        return testDirectory.toFile().listFiles().isEmpty()
    }

    fun localTestContentDelete() {

        val testDir = testDirectory.toFile()

        if (testDir.exists()) {
            log.info("Emptying temporary directory for test content: ${testDirectory}")

            deleteContentOf(testDir)
        }
    }

    private fun deleteContentOf(dir: File) {
        dir
                .walkTopDown()
                .maxDepth(1)
                .filterNot { it == dir }
                .forEach {
                    log.info { "Deleting local item: $it" }
                    it.deleteRecursively()
                }
    }

    private fun moveDirContent(srcDir: Path, destDir: Path) {
        srcDir.toFile()
                .walkTopDown()
                .maxDepth(1)
                .filterNot { it.toPath() == srcDir }
                .forEach {
                    val targetPath = Paths.get(destDir.toString(), it.toPath().fileName.toString())
                    log.info { "Moving local item: $it to $targetPath" }
                    Files.move(it.toPath(), targetPath)
                }
    }

    private fun liveFilesListQuery() = retryOnUsageLimitsException { drive.files().list().setQ("trashed = false") }

    private fun children(parentId: String?, fileList: List<com.google.api.services.drive.model.File>): List<FsItem> {

        val children: MutableList<FsItem> = mutableListOf()

        val childrenRaw = fileList.filter { it.parents.contains(parentId) }

        children += childrenRaw
                .filter { it.mimeType != GOOGLE_DRIVE_FOLDER_MIMETYPE }
                .map { fileItem ->

                    val fileContent = contentOfFileWith(fileItem.id)

                    FsStructure.fle(fileItem.name, fileContent)
                }

        children += childrenRaw
                .filter { it.mimeType == GOOGLE_DRIVE_FOLDER_MIMETYPE }
                .map { FsStructure.dir(it.name, *children(it.id, fileList).toTypedArray()) }

        return children
    }

    private fun contentOfFileWith(fileId: String?): ByteArray? {
        val outputStream = ByteArrayOutputStream()

        outputStream.use {
            retryOnUsageLimitsException { drive.files().get(fileId).executeMediaAndDownloadTo(outputStream) }
        }

        return outputStream.toByteArray()
    }

    fun localStructure(): FsStructure {
        return FsStructure.readFrom(testDirectory)
    }

    fun findTopLevelFolderIdByName(folderName: String): String? = retryOnUsageLimitsException { // todo return folder object
        drive
                // https://developers.google.com/drive/api/v3/search-parameters
                .files()
                .list()
                .setSpaces("drive")
                .setQ("mimeType='$GOOGLE_DRIVE_FOLDER_MIMETYPE' and 'root' in parents and name = '$folderName'")
                .execute()
                .files
                .first()
                .id
    }

    private fun <R> retryOnUsageLimitsException(action: () -> R): R = RetryingExecutor.retryOnException(
            action = action,
            timeOut = Duration.ofSeconds(10),
            isRetryableExceptionPredicate = { throwable ->
                throwable is GoogleJsonResponseException
                        && throwable.statusCode == 403
                        && with(throwable.details.errors) {
                    isNotEmpty()
                            && this[0].domain == "usageLimits"
                            && this[0].reason == "userRateLimitExceeded"
                }
            },
            actionOnExpiration = { log.error { "expired" } }
    )
}
