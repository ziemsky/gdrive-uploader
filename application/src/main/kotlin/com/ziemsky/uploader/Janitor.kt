package com.ziemsky.uploader

import com.ziemsky.uploader.model.local.LocalFile
import mu.KotlinLogging

private val log = KotlinLogging.logger {}
class Janitor {

    fun cleanupSecuredFile(localFile: LocalFile) {
        log.info { "Deleting $localFile" }

        localFile.file.delete()
    }
}
