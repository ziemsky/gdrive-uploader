package com.ziemsky.uploader

interface StatsReporter {
    fun reportStatsForSecuredFiles(securedFiles: Set<SecuredFileSummary>)
}