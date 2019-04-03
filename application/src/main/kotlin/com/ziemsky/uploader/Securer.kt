package com.ziemsky.uploader

import com.ziemsky.uploader.model.local.LocalFile
import com.ziemsky.uploader.model.repo.RepoFolder
import com.ziemsky.uploader.model.repo.RepoFolderName
import mu.KotlinLogging
import java.time.Clock
import java.time.Instant.now

private val log = KotlinLogging.logger {}

class Securer(
        private val remoteRepository: RemoteRepository,
        private val domainEventsNotifier: DomainEventsNotifier,
        private val clock: Clock
) {

    fun secure(localFile: LocalFile) {

        log.debug { "Securing $localFile" }

        val dailyRepoFolder = RepoFolder.from(localFile.date)

        val instantStart = now(clock)

        remoteRepository.upload(dailyRepoFolder, localFile)

        val instantStop = now(clock)

        domainEventsNotifier.notifyFileSecured(SecuredFileSummary(instantStart, instantStop, localFile))
    }


    fun ensureRemoteDailyFolder(localFile: LocalFile) {

        val dailyRepoFolderName = RepoFolderName.from(localFile.date)

        if (remoteRepository.topLevelFolderWithNameAbsent(dailyRepoFolderName)) {
            remoteRepository.createFolderWithName(dailyRepoFolderName)
            log.debug { "Created folder $dailyRepoFolderName" }
            domainEventsNotifier.notifyNewRemoteDailyFolderCreated(dailyRepoFolderName)
        }

    }


//    upload files
//      detect all daily folders based on file names
//      create matching remote folders if don't exist
//      upload each file into corresponding daily folder
//      delete local files once successfully uploaded
//      rotate remote folders files by deleting the oldest ones until configured max number left
//      best check for combined size and delete oldest files until configured quota is reached?
//
//      could do after each batch but better to do after the last batch
//
//      What about back-pressure depending on 'request rate exceeded error'? let's see if that gets reached at all,
//      given we're now batching requests.
//      Options:
//      a) GDrive client blocks and handles retries itself
//      b) fails and caller handles retries and blocks
//      c) b) + caller is preceded by some valve that it notifies that batch needs retrying with valve handling the retries
//        https://developers.google.com/drive/api/v3/about-sdk
//        https://developers.google.com/drive/api/v3/handle-errors#errors_and_suggested_actions - see expotential back-off
//        https://developers.google.com/api-client-library/java/
//
//      Given that batch is now a single message, the valve should be able to handle the retries (with expotential
//      back-off, responding to a message
//
//      get remaining space:
//        https://developers.google.com/drive/api/v3/reference/about/get
//        https://developers.google.com/drive/api/v3/reference/about#resource - see storageQuota.* fields

}