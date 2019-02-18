package com.ziemsky.uploader

import com.ziemsky.uploader.conf.UploaderConfig
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import

@Import(UploaderConfig::class)
class UploaderApplication

fun main(args: Array<String>) {
    runApplication<UploaderApplication>(*args)
}