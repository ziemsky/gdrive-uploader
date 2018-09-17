package com.ziemsky.uploader

import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import mu.KotlinLogging
import java.io.File

private val log = KotlinLogging.logger {}

class GDriveFileRepository(val drive: Drive) : FileRepository {

    override fun upload(files: List<File>) {

        log.info("UPLOADING ${files.size} files: $files")

        // Note: has to upload one by one because Google Drive API doesn't support batching of media uploads nor
        // downloads. See https://developers.google.com/drive/api/v3/batch

        files.forEach { localFile: File? ->
            log.info("UPLOADING ${localFile?.path} (${localFile?.length()})")

            val gDriveFile = com.google.api.services.drive.model.File()
            gDriveFile.name = localFile?.name

            val mediaContent = FileContent(null, localFile)

            drive.files().create(gDriveFile, mediaContent).execute()

            // todo media type
            // is metadata needed or will the client discover and populate it itself?")
            // val content: FileContent = FileContent("image/jpeg", localFile)
            // drive.files().create(gDriveFile, content)
        }
    }
}