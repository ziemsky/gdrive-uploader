package com.ziemsky.uploader

import org.slf4j.LoggerFactory
import java.io.File

class GDriveUploader : Uploader {

    companion object {
        val log = LoggerFactory.getLogger(GDriveUploader.javaClass)
    }

    override fun upload(files: List<File>) {
        log.info("PAYLOAD[${files.size}]: $files")
    }
}