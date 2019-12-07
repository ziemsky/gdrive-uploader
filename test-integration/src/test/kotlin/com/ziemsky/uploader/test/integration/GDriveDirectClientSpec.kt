package com.ziemsky.uploader.test.integration

import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import com.ziemsky.fsstructure.FsStructure.*
import com.ziemsky.uploader.securing.infrastructure.googledrive.GDriveDirectClient
import com.ziemsky.uploader.securing.infrastructure.googledrive.GDriveRetryingClient
import com.ziemsky.uploader.securing.infrastructure.googledrive.model.GDriveFolder
import com.ziemsky.uploader.securing.model.local.LocalFile
import com.ziemsky.uploader.securing.model.remote.RemoteDailyFolder
import com.ziemsky.uploader.test.integration.config.IntegrationTestConfig
import com.ziemsky.uploader.test.shared.data.TestFixtures
import io.kotlintest.*
import io.kotlintest.specs.BehaviorSpec
import org.springframework.test.context.ContextConfiguration
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.time.LocalDate

@ContextConfiguration(classes = [(IntegrationTestConfig::class)])
class GDriveDirectClientSpec(val drive: Drive,
                             val testFixtures: TestFixtures,
                             testDirectory: Path
) : BehaviorSpec() {
    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf

    lateinit var gDriveClient: GDriveRetryingClient

    // todo consider slimming down pre-existing remote folder structure - too many unnecessary items to create, too much time wasted? different set for different tests?

    init {

        Given("a remote structure of daily folders exist") {
            val rootFolder = testInitialisedAndDriveCreated()

            testFixtures.remoteStructureCreateFrom(rootFolder.id, create(
                    dir("2018-09-01",
                            fle("nested file"),
                            dir("nested dir"),
                            dir("2018-09-05")),
                    dir("2018-09-02",
                            fle("nested file"),
                            dir("nested dir"),
                            dir("2018-09-06")),
                    dir("2018-09-03",
                            fle("nested file"),
                            dir("nested dir"),
                            dir("2018-09-07")),

                    dir("mis-matching folder",
                            fle("another nested file"),
                            dir("another nested dir"),
                            dir("2018-09-08")),

                    fle("2018-09-04"),
                    fle("mis-matching top-level file")
            ))

            When("retrieving child folders of given folder") {

                val actualFolders = gDriveClient.childFoldersOf(rootFolder)

                Then("returns existing top level folders") {
                    actualFolders should containExactlyFoldersWithIdsInAnyOrder(
                            "2018-09-01",
                            "2018-09-02",
                            "2018-09-03"
                    )
                }
            }

            When("removing an existing folder") {

                val targetFolderName = "2018-09-01"
                val targetFolderId = testFixtures.findChildFolderIdByName(rootFolder.id, targetFolderName)

                gDriveClient.deleteFolder(GDriveFolder(targetFolderName, targetFolderId!!))


                Then("deletes requested folder with its content, leaving other content intact, and updates local cache") {

                        testFixtures.remoteStructureWithin(rootFolder.id) shouldBe create(
                                dir("2018-09-02",
                                        fle("nested file"),
                                        dir("nested dir"),
                                        dir("2018-09-06")),
                                dir("2018-09-03",
                                        fle("nested file"),
                                        dir("nested dir"),
                                        dir("2018-09-07")),

                                dir("mis-matching folder",
                                        fle("another nested file"),
                                        dir("another nested dir"),
                                        dir("2018-09-08")),
                                fle("2018-09-04"),
                                fle("mis-matching top-level file")
                        )
                }
            }

            When("creating new folder with given name as a child of another folder") {

                val expectedFolderName = "2020-12-10"

                val actualGDriveFolder = gDriveClient.createTopLevelFolder(rootFolder.id, expectedFolderName)

                Then("new remote folder gets created alongside existing ones") {

                    testFixtures.remoteStructureWithin(rootFolder.id) shouldBe create(
                            dir(expectedFolderName), // newly added folder

                            dir("2018-09-01",
                                    fle("nested file"),
                                    dir("nested dir"),
                                    dir("2018-09-05")),
                            dir("2018-09-02",
                                    fle("nested file"),
                                    dir("nested dir"),
                                    dir("2018-09-06")),
                            dir("2018-09-03",
                                    fle("nested file"),
                                    dir("nested dir"),
                                    dir("2018-09-07")),

                            dir("mis-matching folder",
                                    fle("another nested file"),
                                    dir("another nested dir"),
                                    dir("2018-09-08")),

                            fle("2018-09-04"),
                            fle("mis-matching top-level file")
                    )

                    actualGDriveFolder.name shouldBe expectedFolderName
                }
            }
        }

        Given("a single file to upload") {
            val rootFolder = testInitialisedAndDriveCreated()

            val testFileName = "20180901120000-00-front.jpg"


            val targetFolder = RemoteDailyFolder.from(LocalDate.of(2018, 9, 1))

            val expectedStructure = create(
                    dir(targetFolder.name.toString(),
                            fle(testFileName)
                    )
            )

            testFixtures.localStructureCreateFrom(create(fle(testFileName)))

            val localFile = LocalFile(Paths.get(testDirectory.toString(), testFileName).toFile())

            And("existing, corresponding daily folder") {

                testFixtures.remoteStructureCreateFrom(rootFolder.id, create(
                        dir(targetFolder.name.toString())
                ))

                val existingDailyFolderId = testFixtures.findChildFolderIdByName(rootFolder.id, targetFolder.name.toString())

                val gDriveFile = com.google.api.services.drive.model.File()
                gDriveFile.name = localFile.nameLocal().raw
                gDriveFile.parents = listOf(existingDailyFolderId)

                val mediaContent = FileContent(null, localFile.raw())

                When("uploading the file") {

                    gDriveClient.upload(gDriveFile, mediaContent)


                    Then("the file is uploaded to the existing daily folder") {

                        testFixtures.remoteStructure(rootFolder.id) shouldBe expectedStructure
                    }
                }
            }
        }
    }

    private fun containExactlyFoldersWithIdsInAnyOrder(vararg expectedFolderNames: String) = object: Matcher<List<GDriveFolder>> {
        override fun test(actualFolders: List<GDriveFolder>): Result {

            val actualFolderIdsList = actualFolders.map(GDriveFolder::name).sorted()
            val expectedFoldersNamesList = expectedFolderNames.asList().sorted()

            return Result (
                    actualFolderIdsList == expectedFoldersNamesList,
                    "Should contain folders with names $expectedFoldersNamesList but was $actualFolderIdsList",
                    "Should not contain folders with names: $expectedFoldersNamesList but was $actualFolderIdsList"
            )
        }
    }

    private fun testInitialisedAndDriveCreated(): GDriveFolder {
        testFixtures.localTestContentDelete()
        testFixtures.remoteStructureDelete()

        val rootFolder = testFixtures.createRootFolder("rootFolder")

        gDriveClient = GDriveRetryingClient(GDriveDirectClient(drive), Duration.ofMinutes(1))

        return rootFolder
    }

}