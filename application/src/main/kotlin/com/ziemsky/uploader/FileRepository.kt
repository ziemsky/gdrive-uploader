package com.ziemsky.uploader

import java.io.File

interface FileRepository {
    fun upload(files: List<File>)
}
