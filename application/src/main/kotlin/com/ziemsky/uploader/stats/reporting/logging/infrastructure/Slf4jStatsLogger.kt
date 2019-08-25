package com.ziemsky.uploader.stats.reporting.logging.infrastructure

import com.ziemsky.uploader.securing.model.SecuredFilesBatchStats
import com.ziemsky.uploader.stats.reporting.logging.StatsLogRenderer
import com.ziemsky.uploader.stats.reporting.logging.StatsLogger
import mu.KotlinLogging


private val log = KotlinLogging.logger {}

class Slf4jStatsLogger(private val statsLogRenderer: StatsLogRenderer) : StatsLogger {

    override fun log(securedFilesBatchStats: SecuredFilesBatchStats) {

        val renderedLogLines = statsLogRenderer.render(securedFilesBatchStats)

        renderedLogLines.forEach { line -> log.info(line.text) }
    }
}