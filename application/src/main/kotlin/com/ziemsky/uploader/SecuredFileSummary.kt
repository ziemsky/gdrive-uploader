package com.ziemsky.uploader

import com.ziemsky.uploader.model.local.LocalFile
import java.time.Instant

data class SecuredFileSummary(
        val uploadStart: Instant,
        val uploadEnd: Instant,
        val securedFile: LocalFile
)