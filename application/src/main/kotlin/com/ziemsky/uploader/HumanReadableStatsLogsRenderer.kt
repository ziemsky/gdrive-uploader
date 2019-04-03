package com.ziemsky.uploader

import com.jakewharton.byteunits.BinaryByteUnit
import com.ziemsky.uploader.Lines.Lines
import org.apache.commons.lang3.time.DurationFormatUtils
import java.time.Duration

class HumanReadableStatsLogsRenderer : StatsLogRenderer {

    override fun render(securedFilesBatchStats: SecuredFilesBatchStats): Lines {
        return Lines(
                "secured files count: ${securedFilesBatchStats.filesCount}",
                "    upload duration: ${formatDuration(securedFilesBatchStats.duration)}",
                "        upload size: ${formatByteSize(securedFilesBatchStats.totalFilesSizeInBytes)}",
                "       upload speed: ${formatByteSize(securedFilesBatchStats.securedBytesPerSec)}/s",
                "       upload start: ${securedFilesBatchStats.start}",
                "         upload end: ${securedFilesBatchStats.end}"
        )
    }

    private fun formatByteSize(byteSize: Long) = BinaryByteUnit.format(byteSize)

    private fun formatDuration(duration: Duration) = DurationFormatUtils.formatDurationHMS(duration.toMillis())
}