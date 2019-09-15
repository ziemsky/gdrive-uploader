package com.ziemsky.uploader.securing.infrastructure.googledrive

import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import com.ziemsky.uploader.securing.infrastructure.googledrive.model.GDriveFolder
import com.ziemsky.uploader.securing.model.local.LocalFile
import com.ziemsky.uploader.securing.model.remote.RemoteFolder
import com.ziemsky.uploader.securing.model.remote.RemoteFolderName
import io.kotlintest.IsolationMode
import io.kotlintest.assertSoftly
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.BehaviorSpec
import io.mockk.MockKVerificationScope
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.File
import java.time.LocalDate

class GDriveRemoteRepositorySpec : BehaviorSpec() {
    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf

    // todo restructure: let individual test cases set their own preconditions

    init {
        val drive: Drive = mockk(relaxed = true)
        val gDriveClient: GDriveClient = mockk(relaxed = true)


        Given("Instance has been created") {
            val gDriveRemoteRepository = GDriveRemoteRepository(drive, gDriveClient)

            And("Instance has not been initialised, yet") {
                When("Retrieving daily folder count") {
                    Then("The count is zero") {

                        gDriveRemoteRepository.dailyFolderCount() shouldBe 0
                        verify(exactly = 0) { gDriveClient.getTopLevelDailyFolders() }

                    }
                }
            }

            And("Remote daily folders exist") {

                every {
                    gDriveClient.getTopLevelDailyFolders()
                } returns mutableListOf(
                        GDriveFolder("2018-09-02", "folder_b_id______________________"),
                        GDriveFolder("2018-09-03", "folder_c_id______________________"),
                        GDriveFolder("2018-09-01", "folder_a_id______________________")
                )

                When("Instance is being initialised") {

                    gDriveRemoteRepository.init()

                    Then("retrieves and caches existing remote daily folders") {
                        gDriveRemoteRepository.dailyFolderCount() shouldBe 3
                        gDriveRemoteRepository.findOldestDailyFolder() shouldBe RemoteFolder.from(LocalDate.parse("2018-09-01"))
                    }
                }

                And("Instance has been initialised") {
                    gDriveRemoteRepository.init()

                    When("Checking whether an absent top level folder with given name does not exists") {

                        Then("reports that it does not exist") {
                            gDriveRemoteRepository.topLevelFolderWithNameAbsent(RemoteFolderName.from("2021-12-10")) shouldBe true
                        }
                    }

                    When("Checking whether an existing top level folder with given name does not exists") {

                        Then("Reports that it does exist") {
                            gDriveRemoteRepository.topLevelFolderWithNameAbsent(RemoteFolderName.from("2018-09-01")) shouldBe false
                        }
                    }

                    When("asked for daily folders count") {

                        Then("returns number of existing daily folders, ignoring mis-matching ones") {
                            gDriveRemoteRepository.dailyFolderCount() shouldBe 3
                        }
                    }

                    When("asked for finding oldest daily folder") {

                        Then("returns the oldest of the existing daily folders") {
                            gDriveRemoteRepository.findOldestDailyFolder() shouldBe RemoteFolder.from(LocalDate.parse("2018-09-01"))
                        }
                    }

                    When("asked to removed an existing folder") {

                        gDriveRemoteRepository.deleteDailyFolder(RemoteFolder.from(LocalDate.parse("2018-09-01")))

                        Then("deletes requested folder with its content, leaving other content intact, and updates local cache") {

                            assertSoftly {
                                verify(exactly = 1) { gDriveClient.deleteFolder(GDriveFolder("2018-09-01", "folder_a_id______________________")) }

                                gDriveRemoteRepository.dailyFolderCount() shouldBe 2
                                gDriveRemoteRepository.findOldestDailyFolder() shouldBe RemoteFolder.from(LocalDate.parse("2018-09-02"))
                            }
                        }
                    }

                    When("creating new daily folder with given name") {
                        val folderToCreateName = "2020-12-10"

                        every { gDriveClient.createTopLevelFolder(folderToCreateName) } returns
                                GDriveFolder(folderToCreateName, "1hHY9WdesA_iww2OPmUPKZRkxprkxYROy")

                        gDriveRemoteRepository.createTopLevelFolder(RemoteFolderName.from(folderToCreateName))

                        Then("""new remote folder gets created alongside existing ones
                            |and it gets added to the local cache
                        """.trimMargin()) {

                            verify(exactly = 1) {
                                gDriveClient.createTopLevelFolder(folderToCreateName)
                            }

                            gDriveRemoteRepository.topLevelFolderWithNameAbsent(RemoteFolderName.from(folderToCreateName)) shouldBe false
                        }
                    }

                }
            }

            And("There is a single file to secure") {

                val testFileName = "20180901120000-00-front.jpg"

                val localFile = LocalFile(File(testFileName))

                val targetFolder = RemoteFolder.from(LocalDate.of(2018, 9, 1))


                And("There is an existing, corresponding, remote daily folder") {

                    every {
                        gDriveClient.getTopLevelDailyFolders()
                    } returns mutableListOf(
                            GDriveFolder("2018-09-01", "folder_a_id______________________")
                    )


                    And("The instance has been initialised") {

                        gDriveRemoteRepository.init()

                        val gDriveFile = com.google.api.services.drive.model.File()
                        gDriveFile.name = localFile.nameLocal.raw
                        gDriveFile.parents = listOf("folder_a_id______________________")

                        every { gDriveClient.upload(any(), any()) } returns Unit


                        When("Securing the file") {

                            gDriveRemoteRepository.upload(targetFolder, localFile)

                            Then("The file is uploaded into the existing daily folder") {

                                verify(exactly = 1) {
                                    gDriveClient.upload(gDriveFile, fileContentWithMatchingName(localFile))
                                }
                            }
                        }
                    }
                }

                And("There is no corresponding daily folder") {

                    every {
                        gDriveClient.getTopLevelDailyFolders()
                    } returns mutableListOf(
                            GDriveFolder("2018-09-02", "folder_b_id______________________"),
                            GDriveFolder("2018-09-03", "folder_c_id______________________")
                    )

                    gDriveRemoteRepository.init()

                    When("Securing the file") {

                        val actualException = shouldThrow<IllegalArgumentException> {
                            // todo custom exception
                            gDriveRemoteRepository.upload(targetFolder, localFile)
                        }

                        Then("Error is reported") {
                            actualException.message shouldBe "Target folder ${targetFolder.name} does not exist"
                        }
                    }
                }
            }
        }
    }

    private fun MockKVerificationScope.fileContentWithMatchingName(localFile: LocalFile): FileContent =
            match { it.file.name == localFile.nameLocal.raw }
}