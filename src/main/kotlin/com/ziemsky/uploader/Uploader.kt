package com.ziemsky.uploader

import java.io.File

interface Uploader {

    fun upload(files: List<File>)
}