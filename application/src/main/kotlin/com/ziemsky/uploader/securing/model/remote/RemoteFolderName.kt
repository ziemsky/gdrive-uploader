package com.ziemsky.uploader.securing.model.remote

import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class RemoteFolderName private constructor(private val raw: String) {

    override fun toString(): String = raw

    companion object {

        fun from(date: LocalDate): RemoteFolderName {
            return RemoteFolderName(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
        }

        fun from(date: String): RemoteFolderName {
            return from(LocalDate.parse(date))
        }
    }
}
