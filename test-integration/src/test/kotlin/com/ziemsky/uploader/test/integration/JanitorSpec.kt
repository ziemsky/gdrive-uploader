package com.ziemsky.uploader.test.integration

import com.ziemsky.fsstructure.FsStructure.create
import com.ziemsky.fsstructure.FsStructure.fle
import com.ziemsky.gdriveuploader.test.shared.data.TestFixtures
import com.ziemsky.uploader.Janitor
import com.ziemsky.uploader.model.local.LocalFile
import com.ziemsky.uploader.test.integration.config.IntegrationTestConfig
import io.kotlintest.shouldBe
import io.kotlintest.specs.BehaviorSpec
import org.springframework.test.context.ContextConfiguration
import java.io.File
import java.nio.file.Path

@ContextConfiguration(classes = [(IntegrationTestConfig::class)])
class JanitorSpec(testFixtures: TestFixtures,
                  testDirectory: Path
) : BehaviorSpec({

    given("Existing files, some to keep, one to cleanup") {

        testFixtures.cleanupLocalTestDir()

        val janitor = Janitor()

        val localFileToCleanUp = LocalFile(File(testDirectory.toString(), "20180908120000-01-front.jpg")) // todo get file from fle?
        val fileToCleanUp = fle("20180908120000-01-front.jpg")

        val fileToKeepA = fle("20180908120000-00-front.jpg")
        val fileToKeepB = fle("20180908120000-02-front.jpg")
        val fileToKeepC = fle("20180908120000-03-front.jpg")

        testFixtures.localStructureCreateFrom(create(
                fileToKeepA,
                fileToCleanUp,
                fileToKeepB,
                fileToKeepC
        ))


        `when`("cleaning given file") {

            janitor.cleanupSecuredFile(localFileToCleanUp)


            then("removes given file leaving adjacent ones intact") {
                testFixtures.localStructure() shouldBe create(
                        fileToKeepA,
                        fileToKeepB,
                        fileToKeepC
                )
            }
        }
    }

    // todo more test cases - no file?
})