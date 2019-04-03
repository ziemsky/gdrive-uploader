package com.ziemsky.uploader

import com.ziemsky.uploader.model.local.LocalFile
import java.time.Instant

class StatsCalculator {
    fun calculateStatsFor(securedFileSummaries: Set<SecuredFileSummary>): SecuredFilesBatchStats { // exception on empty set?
        return SecuredFilesBatchStats(
                securedFileSummaries.size,
                minStartTime(securedFileSummaries),
                maxStartTime(securedFileSummaries),
                totalFilesSizeInBytes(securedFileSummaries)
        )
    }

    private fun minStartTime(securedFileSummaries: Set<SecuredFileSummary>): Instant =
            securedFileSummaries.minBy(SecuredFileSummary::uploadStart)?.uploadStart ?: Instant.MIN // todo null value? exception?

    private fun maxStartTime(securedFileSummaries: Set<SecuredFileSummary>): Instant =
            securedFileSummaries.maxBy(SecuredFileSummary::uploadEnd)?.uploadEnd ?: Instant.MIN // todo null value? exception?

    private fun totalFilesSizeInBytes(securedFileSummaries: Set<SecuredFileSummary>): Long =
            securedFileSummaries
                    .map(SecuredFileSummary::securedFile)
                    .fold(0, { totalFilesSizeInBytes: Long, securedFile: LocalFile -> totalFilesSizeInBytes + securedFile.sizeInBytes })
}