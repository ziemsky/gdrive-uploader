package com.ziemsky.uploader

import org.slf4j.LoggerFactory
import java.io.File

class GDriveRepository : Repository {

    companion object {
        val log = LoggerFactory.getLogger(GDriveRepository::class.java)
    }

    override fun upload(files: List<File>) {
        log.info("PAYLOAD[${files.size}]: $files")
    }
}