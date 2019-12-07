package com.ziemsky.uploader.securing.model.local

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.time.LocalDate
import java.time.ZoneId

class LocalFile(private val file: File) {

    fun date(): LocalDate = creationDateOf(file)

    fun nameLocal(): LocalFileName = LocalFileName(file.name)

    fun path(): Path = file.toPath()

    fun sizeInBytes(): Long = file.length()

    fun raw(): File = file

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
