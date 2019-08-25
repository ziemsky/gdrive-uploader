package com.ziemsky.uploader.securing

import com.ziemsky.uploader.UploaderAbstractBehaviourSpec
import com.ziemsky.uploader.securing.model.SecuredFileSummary
import com.ziemsky.uploader.securing.model.local.LocalFile
import com.ziemsky.uploader.securing.model.remote.RemoteFolder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class SecurerSpec : UploaderAbstractBehaviourSpec() {

    init {

        Given("A file to secure") {

            val remoteRepository: RemoteRepository = mockk(relaxed = true)
            val domainEventsNotifier: DomainEventsNotifier = mockk(relaxed = true)

            val clock = Clock.fixed(Instant.parse("2019-08-19T20:33:00Z"), ZoneId.of("UTC"))

            val securer = Securer(remoteRepository, domainEventsNotifier, clock)

            val localFile = LocalFile(File("20180901120000-00-front.jpg"))

            val dailyFolder = RemoteFolder.from(localFile.date)


            When("securing file") {

                securer.secure(localFile)

                Then("""file gets secured in the repository in corresponding daily folder
                    |and secured file gets reported
                """.trimMargin()) {


                    val expectedSecuredFileSummary = SecuredFileSummary(
                            uploadStart = clock.instant(),
                            uploadEnd = clock.instant(),
                            securedFile = localFile
                    )

                    verify { remoteRepository.upload(dailyFolder, localFile) }
                    verify { domainEventsNotifier.notifyFileSecured(expectedSecuredFileSummary) }
                }
            }


            And("no remote daily folder existing for the day of the file") {

                every { remoteRepository.topLevelFolderWithNameAbsent(dailyFolder.name) } returns true


                When("asked to ensure daily folder available in the repository for the file") {

                    securer.ensureRemoteDailyFolder(localFile)

                    Then("""daily folder gets created
                        |and the creation of the folder gets reported
                        """.trimMargin()) {

                        verifyOrder {
                            remoteRepository.createFolderWithName(dailyFolder.name)
                            domainEventsNotifier.notifyNewRemoteDailyFolderCreated(dailyFolder.name)
                        }
                    }
                }
            }


            And("remote daily folder existing for the day of the file") {

                every { remoteRepository.topLevelFolderWithNameAbsent(dailyFolder.name) } returns false

                When("asked to ensure daily folder available in the repository for the file") {

                    securer.ensureRemoteDailyFolder(localFile)

                    Then("""no folder creation is attempted
                        |and no event gets emitted""".trimMargin()) {

                        verify(exactly = 0) { remoteRepository.createFolderWithName(any()) }
                        verify(exactly = 0) { domainEventsNotifier.notifyNewRemoteDailyFolderCreated(any()) }
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