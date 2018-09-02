package com.ziemsky.uploader

import java.io.File

interface Repository {
    fun upload(files: List<File>)
}
