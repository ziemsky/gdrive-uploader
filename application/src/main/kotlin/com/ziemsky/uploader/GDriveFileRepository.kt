package com.ziemsky.uploader

import org.slf4j.LoggerFactory
import java.io.File

public class GDriveFileRepository: FileRepository {

    companion object {
        val log = LoggerFactory.getLogger(GDriveFileRepository::class.java)
    }

    override fun upload(files: List<File>) {
        log.info("PAYLOAD[${files.size}]: $files")
    }
}