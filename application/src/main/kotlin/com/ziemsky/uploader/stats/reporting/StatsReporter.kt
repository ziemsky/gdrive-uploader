package com.ziemsky.uploader.stats.reporting

import com.ziemsky.uploader.securing.model.SecuredFileSummary

interface StatsReporter {
    fun reportStatsForSecuredFiles(securedFiles: Set<SecuredFileSummary>)
}