package com.ziemsky.uploader.stats.reporting.logging

import com.ziemsky.uploader.securing.model.SecuredFilesBatchStats
import com.ziemsky.uploader.stats.reporting.logging.model.Line

interface StatsLogRenderer {
    fun render(securedFilesBatchStats: SecuredFilesBatchStats): List<Line>
}