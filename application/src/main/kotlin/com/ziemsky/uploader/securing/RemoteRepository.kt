package com.ziemsky.uploader.securing

import com.ziemsky.uploader.securing.model.local.LocalFile
import com.ziemsky.uploader.securing.model.remote.RemoteFolder
import com.ziemsky.uploader.securing.model.remote.RemoteFolderName

interface RemoteRepository { // todo rename

    fun upload(targetFolder: RemoteFolder, localFile: LocalFile)

    fun dailyFolderCount(): Int

    fun findOldestDailyFolder(): RemoteFolder?

    fun deleteDailyFolder(remoteFolder: RemoteFolder)

    fun topLevelFolderWithNameAbsent(folderName: RemoteFolderName): Boolean

    fun createTopLevelFolder(remoteFolderName: RemoteFolderName)
}
