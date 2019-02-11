package com.ziemsky.uploader

import com.ziemsky.uploader.model.local.LocalFile
import com.ziemsky.uploader.model.repo.RepoFolder
import com.ziemsky.uploader.model.repo.RepoFolderName

interface FileRepository {

    fun upload(targetFolder: RepoFolder, localFile: LocalFile)

    fun folderWithNameAbsent(folderName: RepoFolderName): Boolean

    fun createFolderWithName(repoFolderName: RepoFolderName)
}
