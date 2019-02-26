package com.ziemsky.uploader.test.integration

import com.google.api.services.drive.Drive
import com.ziemsky.fsstructure.FsStructure.*
import com.ziemsky.gdriveuploader.test.shared.data.TestFixtures
import com.ziemsky.uploader.GDriveRemoteRepository
import com.ziemsky.uploader.model.local.LocalFile
import com.ziemsky.uploader.model.repo.RepoFolder
import com.ziemsky.uploader.test.integration.config.IntegrationTestConfig
import io.kotlintest.IsolationMode
import io.kotlintest.assertSoftly
import io.kotlintest.shouldBe
import io.kotlintest.specs.BehaviorSpec
import mu.KotlinLogging
import org.springframework.test.context.ContextConfiguration
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDate

private val log = KotlinLogging.logger {}

@ContextConfiguration(classes = [(IntegrationTestConfig::class)])
class GDriveRemoteRepositorySpec(val drive: Drive,
                                 val testFixtures: TestFixtures,
                                 testDirectory: Path
) : BehaviorSpec() {

    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf

    lateinit var gDriveRemoteRepository: GDriveRemoteRepository

    init {
        Given("a single file to secure") {
            testInitialisedAndDriveCreated()

            val testFileName = "20180901120000-00-front.jpg"


            val targetFolder = RepoFolder.from(LocalDate.of(2018, 9, 1))

            val expectedStructure = create(
                    dir("2018-09-01",
                            fle(testFileName)
                    )
            )

            testFixtures.localStructureCreateFrom(create(fle(testFileName)))

            val localFile = LocalFile(Paths.get(testDirectory.toString(), testFileName).toFile())

            And("existing, corresponding daily folder") {

                testFixtures.remoteStructureCreateFrom(create(
                        dir(targetFolder.name.toString())
                ))

                gDriveRemoteRepository.init()

                When("securing the file") {

                    gDriveRemoteRepository.upload(targetFolder, localFile)


                    Then("the file is uploaded to the existing daily folder") {

                        testFixtures.remoteStructure() shouldBe expectedStructure
                    }
                }
            }

            And("there is no corresponding daily folder") {

                gDriveRemoteRepository.init()

                When("securing the file") {

                    gDriveRemoteRepository.upload(targetFolder, localFile)


                    Then("a new daily folder is created and the file uploaded to it") {

                        testFixtures.remoteStructure() shouldBe expectedStructure
                    }
                }
            }
        }

        Given("instance has been created") {
            testInitialisedAndDriveCreated()

            When("instance has not been initialised") {

                Then("local cache of remote daily folders is empty") {
                    gDriveRemoteRepository.dailyFolderCount() shouldBe 0
                }
            }

            And("remote daily folders exist") {

                testFixtures.remoteStructureCreateFrom(create(
                        dir("2018-09-01",
                                fle("nested file"),
                                dir("nested dir")),
                        dir("2018-09-02",
                                fle("nested file"),
                                dir("nested dir")),
                        dir("2018-09-03",
                                fle("nested file"),
                                dir("nested dir")),

                        dir("mis-matching-folder",
                                fle("nested file"),
                                dir("nested dir")),
                        fle("mis-matching top-level file")
                ))

                When("instance has been initialised") {

                    gDriveRemoteRepository.init()

                    Then("retrieves and caches existing remote daily folders") {
                        gDriveRemoteRepository.dailyFolderCount() shouldBe 3
                        gDriveRemoteRepository.findOldestDailyFolder() shouldBe RepoFolder.from(LocalDate.parse("2018-09-01"))
                    }
                }
            }
        }

        Given("remote daily folders exist") {
            testInitialisedAndDriveCreated()

            testFixtures.remoteStructureCreateFrom(create(
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

                    dir("mis-matching-folder",
                            fle("another nested file"),
                            dir("another nested dir"),
                            dir("2018-09-08")),
                    fle("mis-matching top-level file")
            ))

            gDriveRemoteRepository.init()

            When("asked for daily folders count") {

                Then("returns number of existing daily folders, ignoring mis-matching ones") {
                    gDriveRemoteRepository.dailyFolderCount() shouldBe 3
                }
            }

            When("asked for finding oldest daily folder count") {

                Then("returns the oldest of the existing daily folders") {
                    gDriveRemoteRepository.findOldestDailyFolder() shouldBe RepoFolder.from(LocalDate.parse("2018-09-01"))
                }
            }

            When("asked to removed an existing folder") {

                gDriveRemoteRepository.deleteFolder(RepoFolder.from(LocalDate.parse("2018-09-01")))

                Then("deletes requested folder with its content, leaving other content intact, and updates local cache") {

                    assertSoftly {
                        gDriveRemoteRepository.dailyFolderCount() shouldBe 2
                        gDriveRemoteRepository.findOldestDailyFolder() shouldBe RepoFolder.from(LocalDate.parse("2018-09-02"))

                        testFixtures.remoteStructure() shouldBe create(
                                dir("2018-09-02",
                                        fle("nested file"),
                                        dir("nested dir"),
                                        dir("2018-09-06")),
                                dir("2018-09-03",
                                        fle("nested file"),
                                        dir("nested dir"),
                                        dir("2018-09-07")),

                                dir("mis-matching-folder",
                                        fle("another nested file"),
                                        dir("another nested dir"),
                                        dir("2018-09-08")),
                                fle("mis-matching top-level file")
                        )
                    }
                }
            }
        }

        // todo error cases for when cache is out of synch (deletion, finding, upload, etc.); errors should be reported

        // todo error cases for when more than one folder is found when expecting one (oldest)

        // todo test for caching of remote folders on start; consider making cache an external dependency for storing local state

        // todo for exception-handling cases, extend Drive (it's not final), intercept selected calls to emulate exceptions to test passing of those out of the repo class
    }

    private fun testInitialisedAndDriveCreated() {
        testFixtures.localTestContentDelete()
        testFixtures.remoteStructureDelete()

        gDriveRemoteRepository = GDriveRemoteRepository(drive)
    }
}