package com.ziemsky.gdriveuploader.test.shared.data

import com.google.api.client.googleapis.batch.json.JsonBatchCallback
import com.google.api.client.googleapis.json.GoogleJsonError
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.HttpHeaders
import com.google.api.services.drive.Drive
import com.ziemsky.fsstructure.FsDir
import com.ziemsky.fsstructure.FsItem
import com.ziemsky.fsstructure.FsStructure
import mu.KotlinLogging
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

private val log = KotlinLogging.logger {}
private val GOOGLE_DRIVE_FOLDER_MIMETYPE = "application/vnd.google-apps.folder"

class TestFixtures( // todo make local fixtures handled separately from remote?
        private val testDirectory: Path,
        private val drive: Drive
) {

    fun createLocalFilesWithDates(vararg testFilesInput: TestFilesInput): List<File> {

        val createdFiles: MutableList<File> = mutableListOf()

        testFilesInput.forEach { testFileInput ->
            for (i in 1..testFileInput.count) {
                val createdFile = Files.createFile(Paths.get(testDirectory.toString(), "${testFileInput.date}_${i}.jpg"))
                createdFiles.add(createdFile.toFile())
            }
        }

        return createdFiles
    }

    fun createRemoteFilesWithDates(vararg testFilesInput: TestFilesInput): List<File> {

        val createdFiles: MutableList<File> = mutableListOf()

        testFilesInput.forEach { testFileInput ->
            for (i in 1..testFileInput.count) {
                val createdFile = Files.createFile(Paths.get(testDirectory.toString(), "${testFileInput.date}_${i}.jpg"))
                createdFiles.add(createdFile.toFile())
            }
        }

        return createdFiles
    }

    fun remoteStructure(): FsStructure {
        val root = drive.files().get("root").setFields("id").execute()

        val fullUndeletedGDriveContent = liveFilesListQuery().setFields("files(id, name, mimeType, parents)").execute()

        val fsItems: List<FsItem> = children(root.id, fullUndeletedGDriveContent.files)

        val fsStructure = FsStructure.create(fsItems)

        return fsStructure
    }

    fun remoteStructureDelete() {

        val fullGDriveContent = drive.files().list().setFields("files(id, name, mimeType)").execute()

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

        deletionBatch.execute()
    }

    fun remoteStructureCreateFrom(remoteContent: FsStructure?) {

        val parents = HashMap<FsDir, com.google.api.services.drive.model.File>()

        remoteContent?.walk(
                { dirItem ->

                    log.info { "Creating remote ${dirItem}" }

                    var dir = com.google.api.services.drive.model.File()
                    dir.setName(dirItem.name())
                    dir.setMimeType(GOOGLE_DRIVE_FOLDER_MIMETYPE)

                    if (dirItem.isNested()) {
                        val parentId: String? = parents[dirItem.parent()]?.id
                        dir.setParents(listOf(parentId))
                    }

                    dir = drive.files().create(dir).execute()

                    parents.put(dirItem, dir)
                },
                { fileItem ->
                    log.info { "Creating remote ${fileItem}" }


                    val file = com.google.api.services.drive.model.File()
                    file.setName(fileItem.name())

                    if (fileItem.isNested()) {
                        val parentId: String? = parents[fileItem.parent()]?.id
                        file.setParents(listOf(parentId))
                    }


                    val mediaContent = ByteArrayContent(null, fileItem?.content)

                    drive.files().create(file, mediaContent).execute()
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

    fun cleanupLocalTestDir() { // todo remove, cleanup delegated to Gradle task

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

    private fun liveFilesListQuery() = drive.files().list().setQ("trashed = false")

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
            drive.files().get(fileId).executeMediaAndDownloadTo(outputStream)
        }

        return outputStream.toByteArray()
    }

    fun localStructure(): FsStructure {
        return FsStructure.readFrom(testDirectory)
    }
}