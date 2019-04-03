package com.ziemsky.uploader

class LoggingStatsReporter(
        private val statsCalculator: StatsCalculator,
        private val statsLogger: StatsLogger
) : StatsReporter {

    override fun reportStatsForSecuredFiles(securedFiles: Set<SecuredFileSummary>) {
        securedFiles.toString()

        require(securedFiles.isNotEmpty()) { "Secured files summaries are required but none were provided." }

        val securedFilesBatchStats = statsCalculator.calculateStatsFor(securedFiles)

        statsLogger.log(securedFilesBatchStats)
    }
}