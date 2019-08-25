package com.ziemsky.uploader.securing.model.remote

import java.time.LocalDate

data class RemoteFolder private constructor(val name: RemoteFolderName) {

    override fun toString(): String = name.toString()

    companion object {

        @JvmStatic
        fun from(date: LocalDate) : RemoteFolder {
            return RemoteFolder(RemoteFolderName.from(date))
        }

        fun from(date: String): RemoteFolder {
            return RemoteFolder(RemoteFolderName.from(date))
        }
    }
}
