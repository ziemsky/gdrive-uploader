package com.ziemsky.uploader.securing.model.remote

import java.time.LocalDate

data class RemoteDailyFolder private constructor(val name: RemoteFolderName) {

    override fun toString(): String = name.toString()

    companion object {

        @JvmStatic
        fun from(date: LocalDate) : RemoteDailyFolder {
            return RemoteDailyFolder(RemoteFolderName.from(date))
        }

        fun from(date: String): RemoteDailyFolder {
            return RemoteDailyFolder(RemoteFolderName.from(date))
        }
    }
}
