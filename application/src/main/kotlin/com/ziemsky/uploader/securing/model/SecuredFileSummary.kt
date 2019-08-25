package com.ziemsky.uploader.securing.model

import com.ziemsky.uploader.securing.model.local.LocalFile
import java.time.Instant

data class SecuredFileSummary(
        val uploadStart: Instant,
        val uploadEnd: Instant,
        val securedFile: LocalFile
)