package com.ziemsky.uploader

interface StatsLogger {
    fun log(securedFilesBatchStats: SecuredFilesBatchStats)
}