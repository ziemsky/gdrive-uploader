package com.ziemsky.uploader

import com.ziemsky.uploader.model.local.LocalFile
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

class StatsLogger {

    fun logStatsForSecuredFile(localFile: LocalFile) {
        log.info { "STATS FOR UPLOADED $localFile" }
    }
}