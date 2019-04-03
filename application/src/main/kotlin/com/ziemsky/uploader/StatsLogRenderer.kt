package com.ziemsky.uploader

import com.ziemsky.uploader.Lines.Lines

interface StatsLogRenderer {
    fun render(securedFilesBatchStats: SecuredFilesBatchStats): Lines
}