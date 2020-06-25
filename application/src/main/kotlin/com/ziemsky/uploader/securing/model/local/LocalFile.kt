package com.ziemsky.uploader.securing.model.local

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.time.LocalDate
import java.time.ZoneId

class LocalFile(private val file: File) {

    val sizeInBytes: Long = file.length()

    val date: LocalDate; get() = creationDateOf(file)

    val nameLocal: LocalFileName; get() = LocalFileName(file.name)

    val path: Path; get() = file.toPath()

    val raw: File; get() = file

    fun delete() { file.delete() }

    override fun toString(): String {
        return "LocalFile(file=$file)"
    }

    companion object {

        @JvmStatic
        private fun creationDateOf(file: File): LocalDate =
                Files.readAttributes(file.toPath(), BasicFileAttributes::class.java)
                        .creationTime()
                        .toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
    }
}
