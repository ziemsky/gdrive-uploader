package com.ziemsky.uploader.model.local

import java.io.File
import java.nio.file.Path
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class LocalFile(val file: File) {

    val date: LocalDate = dateFromFileName(file)

    val path: Path get() = file.toPath()

    val name: FileName get() = FileName(file.name)

    companion object {

        @JvmStatic
        private fun dateFromFileName(file: File): LocalDate = LocalDate.parse(
                file.name.substring(0..7),
                DateTimeFormatter.ofPattern("yyyyMMdd")
        )
    }
}
