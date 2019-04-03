package com.ziemsky.uploader

import mu.KotlinLogging


private val log = KotlinLogging.logger {}

class Slf4jStatsLogger(private val statsLogRenderer: StatsLogRenderer) : StatsLogger {

    override fun log(securedFilesBatchStats: SecuredFilesBatchStats) {

        val renderedLogLines = statsLogRenderer.render(securedFilesBatchStats)

        renderedLogLines.stream().forEach { line -> log.info(line.text) }
    }
}