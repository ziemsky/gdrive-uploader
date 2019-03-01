package com.ziemsky.uploader

import com.ziemsky.uploader.model.local.LocalFile
import com.ziemsky.uploader.model.repo.RepoFolder
import io.kotlintest.specs.BehaviorSpec
import io.mockk.mockk
import io.mockk.verify
import java.io.File

class SecurerSpec : BehaviorSpec({

    given("A file to secure") {

        val remoteRepository: RemoteRepository = mockk(relaxed = true)

        val localFile = LocalFile(File("20180901120000-00-front.jpg"))

        val dailyFolder = RepoFolder.from(localFile.date)

        val service = Securer(remoteRepository)


        `when`("securing file") {

            service.secure(localFile)

            then("file gets secured in the repository in corresponding folder") {

                verify { remoteRepository.upload(dailyFolder, localFile) }
            }
        }
    }

// todo remove
//    given("A batch of files to secure, dated across several days with remote folders for some of the files missing") {
//
//        val remoteRepository: RemoteRepository = mockk(relaxed = true)
//
//        val files_2018_12_30 = listOf(
//            File("20181230700000-00-front.jpg"), // 2018-12-30
//            File("20181230700000-01-front.jpg")  // 2018-12-30
//        )
//        val files_2018_12_31 = listOf(
//            File("20181231700000-00-front.jpg"), // 2018-12-31
//            File("20181231700000-00-front.jpg")  // 2018-12-31
//        )
//        val files_2019_01_01 = listOf(
//            File("20190101700000-00-front.jpg"), // 2019-01-01
//            File("20190101700000-00-front.jpg")  // 2019-01-01
//        )
//        val files_2019_01_02 = listOf(
//            File("20190102700000-00-front.jpg"), // 2019-01-02
//            File("20190102700000-00-front.jpg")  // 2019-01-02
//        )
//
//        val files = files_2018_12_30 + files_2018_12_31 + files_2019_01_01 + files_2019_01_02
//
//        val securerService = Securer(remoteRepository)
//
//        `when`("securing files") {
//
//            securerService.secure(files)
//
//            then("""creates daily folders derived from the dates in the files to secure
//                   |and uploads files to corresponding daily folders""".trimMargin()) {
//
//                verifyAll {
//                    remoteRepository.createFolder("2018-12-30")
//                    remoteRepository.createFolder("2018-12-31")
//                    remoteRepository.createFolder("2019-01-01")
//                    remoteRepository.createFolder("2019-01-02")
//                }
//
//                verifyAll {
//                    remoteRepository.upload(files_2018_12_30, "2018-12-30")
//                    remoteRepository.upload(files_2018_12_31, "2018-12-31")
//                    remoteRepository.upload(files_2019_01_01, "2019-01-01")
//                    remoteRepository.upload(files_2019_01_02, "2019-01-02")
//                }
//
//                verify(exactly = 0) { files.any() }
//            }
//        }
//    }
})