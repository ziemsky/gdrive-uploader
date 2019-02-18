package com.ziemsky.uploader.test.integration

import com.google.api.services.drive.Drive
import com.ziemsky.fsstructure.FsStructure.*
import com.ziemsky.gdriveuploader.test.shared.data.TestFixtures
import com.ziemsky.uploader.GDriveFileRepository
import com.ziemsky.uploader.model.local.LocalFile
import com.ziemsky.uploader.model.repo.RepoFolder
import com.ziemsky.uploader.test.integration.config.IntegrationTestConfig
import io.kotlintest.TestCase
import io.kotlintest.shouldBe
import io.kotlintest.specs.BehaviorSpec
import mu.KotlinLogging
import org.springframework.test.context.ContextConfiguration
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDate

private val log = KotlinLogging.logger {}

@ContextConfiguration(classes = [(IntegrationTestConfig::class)])
class GDriveFileRepositorySpec(val drive: Drive,
                               val testFixtures: TestFixtures,
                               testDirectory: Path
) : BehaviorSpec() {

    lateinit var gDriveFileRepository: GDriveFileRepository

    override fun beforeTest(testCase: TestCase) {
        gDriveFileRepository = GDriveFileRepository(drive)
    }

    init {
        given("a single file to secure") {

            log.info { "Created ${gDriveFileRepository}" }

            val testFileName = "20180901120000-00-front.jpg"

            val fileToSecure = LocalFile(Paths.get(testDirectory.toString(), "2018-09-01", testFileName).toFile())

            val expectedStructure = create(
                    dir("2018-09-01",
                            fle(testFileName)
                    )
            ).saveIn(testDirectory)


            and("the folder to secure the file in") {

                val targetFolder = RepoFolder.from(LocalDate.of(2018, 9, 1))

                and("the folder does exist") {
                    testFixtures.remoteStructureDelete()

                    testFixtures.remoteStructureCreateFrom(create(
                            dir("2018-09-01")
                    ))

                    `when`("securing the file") {

                        gDriveFileRepository.upload(targetFolder, fileToSecure)


                        then("the file is uploaded to the corresponding daily folder") {

                            testFixtures.remoteStructure() shouldBe expectedStructure
                        }
                    }
                }


                and("the folder does not exist") {
                    testFixtures.remoteStructureDelete()

                    `when`("securing the file") {

                        gDriveFileRepository.upload(targetFolder, fileToSecure)


                        then("the daily folder is created and ") {

                            testFixtures.remoteStructure() shouldBe expectedStructure
                        }
                    }
                }

            }
        }


        // todo test for caching of remote folders on start

        // todo for exception-handling cases, extend Drive (it's not final), intercept selected calls to emulate exceptions to test passing of those out of the repo class
    }
}