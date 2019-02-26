package com.ziemsky.uploader.model.repo

import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class RepoFolderName private constructor(private val raw: String) {

    override fun toString(): String = raw

    companion object {

        fun from(date: LocalDate): RepoFolderName {
            return RepoFolderName(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
        }

        fun from(date: String): RepoFolderName {
            return from(LocalDate.parse(date))
        }
    }

}
