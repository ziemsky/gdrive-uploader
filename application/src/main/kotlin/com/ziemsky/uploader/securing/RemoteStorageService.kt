package com.ziemsky.uploader.securing

import com.ziemsky.uploader.securing.model.local.LocalFile
import com.ziemsky.uploader.securing.model.remote.RemoteDailyFolder
import com.ziemsky.uploader.securing.model.remote.RemoteFolderName

interface RemoteStorageService { // todo rename

    fun upload(targetDailyFolder: RemoteDailyFolder, localFile: LocalFile)

    fun dailyFolderCount(): Int

    fun findOldestDailyFolder(): RemoteDailyFolder?

    fun deleteDailyFolder(remoteDailyFolder: RemoteDailyFolder)

    fun isTopLevelFolderWithNameAbsent(folderName: RemoteFolderName): Boolean

    fun createTopLevelFolder(remoteFolderName: RemoteFolderName)
}
