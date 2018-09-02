package com.ziemsky.uploader

import java.io.File

class SecurerService(val repository: Repository) {

    fun secure(files: List<File>) {

        repository.upload(files)
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