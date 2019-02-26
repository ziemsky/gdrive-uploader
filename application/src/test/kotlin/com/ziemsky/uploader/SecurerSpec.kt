package com.ziemsky.uploader

import com.ziemsky.uploader.model.local.LocalFile
import com.ziemsky.uploader.model.repo.RepoFolder
import io.kotlintest.IsolationMode
import io.kotlintest.specs.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import java.io.File

class SecurerSpec : BehaviorSpec() {
    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf

    init {

        Given("A file to secure") {

            val remoteRepository: RemoteRepository = mockk(relaxed = true)
            val securerEventReporter: SecurerEventReporter = mockk(relaxed = true)

            val service = Securer(remoteRepository, securerEventReporter)

            val localFile = LocalFile(File("20180901120000-00-front.jpg"))

            val dailyFolder = RepoFolder.from(localFile.date)


            And("remote daily folder existing for the day of the file") {

                every { remoteRepository.topLevelFolderWithNameAbsent(dailyFolder.name) } returns false


                When("securing file") {

                    service.secure(localFile)

                    Then("""file gets secured in the repository in corresponding folder
                        |and no folder creation is attempted""".trimMargin()) {

                        verify(exactly = 0) { remoteRepository.createFolderWithName(any()) }

                        verify { remoteRepository.upload(dailyFolder, localFile) }
                    }
                }
            }


            And("no remote daily folder existing for the day of the file") {

                every { remoteRepository.topLevelFolderWithNameAbsent(dailyFolder.name) } returns true


                When("securing file") {

                    service.secure(localFile)

                    Then("""daily folder gets created
                        |and the file gets secured in the newly created folder
                        |and the creation of the folder gets reported
                        """.trimMargin()) {

                        verifyOrder {
                            remoteRepository.createFolderWithName(dailyFolder.name)
                            remoteRepository.upload(dailyFolder, localFile)
                            securerEventReporter.notifyNewRemoteDailyFolderCreated(dailyFolder.name)
                        }
                    }
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
    }
}