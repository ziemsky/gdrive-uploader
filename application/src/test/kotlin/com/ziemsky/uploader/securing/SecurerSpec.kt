package com.ziemsky.uploader.securing

import com.ziemsky.uploader.UploaderAbstractBehaviourSpec
import com.ziemsky.uploader.securing.model.SecuredFileSummary
import com.ziemsky.uploader.securing.model.local.LocalFile
import com.ziemsky.uploader.securing.model.remote.RemoteDailyFolder
import io.mockk.*
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class SecurerSpec : UploaderAbstractBehaviourSpec() {

    init {

        Given("A file to secure") {

            val remoteStorageService = mockk<RemoteStorageService>(relaxed = true)
            val domainEventsNotifier = mockk<DomainEventsNotifier>(relaxed = true)

            val clock = Clock.fixed(Instant.parse("2019-08-19T20:33:00Z"), ZoneId.of("UTC"))

            val securer = Securer(remoteStorageService, domainEventsNotifier, clock)

            val rawFile = File("20180901120000-00-front.jpg")
            val localFile = LocalFile(rawFile)

            val basicFileAttributes: BasicFileAttributes = mockk()
            val creationTime: FileTime = FileTime.from(Instant.parse("2019-08-19T12:00:00Z"))

            // We are touching File and Files here - which, normally, wouldn't be mocked and whose use would typically make
            // a test like this an 'integrated' one - but we also need a control over the returned file creation time,
            // which does require mocking (of a static method, in this case - oh, the horror!).
            // For this reason, this test is classed as a unit test, after all.
            mockkStatic(Files::class.java.name)
            every { basicFileAttributes.creationTime() } returns creationTime
            every { Files.readAttributes(rawFile.toPath(), BasicFileAttributes::class.java) } returns basicFileAttributes

            val dailyFolder = RemoteDailyFolder.from("2019-08-19")

            And("remote target folder") {

                When("securing the file") {

                    securer.secure(localFile)

                    Then("""file gets secured in the repository in corresponding daily folder
                    |and secured file gets reported
                """.trimMargin()) {

                        val expectedSecuredFileSummary = SecuredFileSummary(
                                uploadStart = clock.instant(),
                                uploadEnd = clock.instant(),
                                securedFile = localFile
                        )

                        verify { remoteStorageService.upload(dailyFolder, localFile) }
                        verify { domainEventsNotifier.notifyFileSecured(expectedSecuredFileSummary) }
                    }
                }


                And("no remote daily folder existing for the day of the file") {

                    every { remoteStorageService.isTopLevelFolderWithNameAbsent(dailyFolder.name) } returns true


                    When("ensuring daily folder available in the repository for the file") {

                        securer.ensureRemoteDailyFolder(localFile)

                        Then("""daily folder gets created
                        |and the creation of the folder gets reported
                        """.trimMargin()) {

                            verifyOrder {
                                remoteStorageService.createTopLevelFolder(dailyFolder.name)
                                domainEventsNotifier.notifyNewRemoteDailyFolderCreated(dailyFolder.name)
                            }
                        }
                    }
                }


                And("remote daily folder existing for the day of the file") {

                    every { remoteStorageService.isTopLevelFolderWithNameAbsent(dailyFolder.name) } returns false

                    When("ensuring daily folder available in the repository for the file") {

                        securer.ensureRemoteDailyFolder(localFile)

                        Then("""no folder creation is attempted
                        |and no event gets emitted""".trimMargin()) {

                            verify(exactly = 0) { remoteStorageService.createTopLevelFolder(any()) }
                            verify(exactly = 0) { domainEventsNotifier.notifyNewRemoteDailyFolderCreated(any()) }
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