package com.ziemsky.uploader.stats.reporting.logging

import com.ziemsky.uploader.securing.model.SecuredFilesBatchStats

interface StatsLogger {
    fun log(securedFilesBatchStats: SecuredFilesBatchStats)
}