package com.ziemsky.uploader.securing.model

import java.time.Duration
import java.time.Instant

data class SecuredFilesBatchStats(
        val filesCount: Int,
        val start: Instant,
        val end: Instant,
        val totalFilesSizeInBytes: Long
) {
    val duration: Duration
        get() = Duration.between(start, end)


    val securedBytesPerSec: Long
        get() = totalFilesSizeInBytes / (duration.toMillis() / 1000)

}
