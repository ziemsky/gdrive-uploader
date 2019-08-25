package com.ziemsky.uploader.stats.reporting.logging

import com.jakewharton.byteunits.BinaryByteUnit
import com.ziemsky.uploader.securing.model.SecuredFilesBatchStats
import com.ziemsky.uploader.stats.reporting.logging.model.Line
import org.apache.commons.lang3.time.DurationFormatUtils
import java.time.Duration

class HumanReadableStatsLogsRenderer : StatsLogRenderer {

    override fun render(securedFilesBatchStats: SecuredFilesBatchStats): List<Line> {
        return arrayOf(
                "---------------------------------------",
                "   Secured files: ${securedFilesBatchStats.filesCount}",
                " Upload duration: ${formatDuration(securedFilesBatchStats.duration)}",
                "     Upload size: ${formatByteSize(securedFilesBatchStats.totalFilesSizeInBytes)}",
                "    Upload speed: ${formatByteSize(securedFilesBatchStats.securedBytesPerSec)}/s",
                "    Upload start: ${securedFilesBatchStats.start}",
                "      Upload end: ${securedFilesBatchStats.end}",
                "---------------------------------------"
        )
                .map { rawLine -> Line(rawLine) }
    }

    private fun formatByteSize(byteSize: Long) = BinaryByteUnit.format(byteSize)

    private fun formatDuration(duration: Duration) = DurationFormatUtils.formatDurationHMS(duration.toMillis())
}