package com.ziemsky.uploader.model.repo

import java.time.LocalDate

data class RepoFolder private constructor(val name: RepoFolderName) {

    override fun toString(): String = name.toString()

    companion object {

        @JvmStatic
        fun from(date: LocalDate) : RepoFolder {
            return RepoFolder(RepoFolderName.from(date))
        }

        fun from(date: String): RepoFolder {
            return RepoFolder(RepoFolderName.from(date))
        }
    }
}
