package com.ziemsky.uploader.test.integration

import com.google.api.services.drive.Drive
import com.ziemsky.fsstructure.FsStructure.create
import com.ziemsky.fsstructure.FsStructure.fle
import com.ziemsky.gdriveuploader.test.shared.data.TestFixtureService
import com.ziemsky.uploader.GDriveFileRepository
import com.ziemsky.uploader.test.integration.config.IntegrationTestConfig
import io.kotlintest.shouldBe
import io.kotlintest.specs.BehaviorSpec
import org.springframework.test.context.ContextConfiguration
import java.io.File
import java.nio.file.Path

@ContextConfiguration(classes = [(IntegrationTestConfig::class)])
class GDriveFileRepositorySpec(drive: Drive,
                               testFixtureService: TestFixtureService,
                               testDirectory: Path
) : BehaviorSpec({

    given("a single file to secure") {
        testFixtureService.remoteStructureDelete()

        val gDriveFileRepository = GDriveFileRepository(drive)

        val randomComponent = System.nanoTime()

        val expectedStructure = create(
                fle("testFileOne_$randomComponent", "random content $randomComponent")
        ).saveIn(testDirectory)

        val filesToSecure: List<File> = testDirectory.toFile().walkTopDown().filter { file -> file.isFile }.toList()


        `when`("securing the file") {

            gDriveFileRepository.upload(filesToSecure)

            then("file is uploaded") {
                // todo use drive to verify state of the remote files (wrapped in some test service?)

                testFixtureService.remoteStructure() shouldBe expectedStructure
            }
        }
    }

    // todo for exception-handling cases, extend Drive (it's not final), intercept selected calls to emulate exceptions to test passing of those out of the repo class
})