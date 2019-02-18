package com.ziemsky.uploader

import com.ziemsky.uploader.model.local.LocalFile
import com.ziemsky.uploader.model.repo.RepoFolder

interface FileRepository {

    fun upload(targetFolder: RepoFolder, localFile: LocalFile)
}
