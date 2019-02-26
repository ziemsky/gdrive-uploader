package com.ziemsky.uploader.test.integration

import com.ziemsky.fsstructure.FsStructure.create
import com.ziemsky.fsstructure.FsStructure.fle
import com.ziemsky.gdriveuploader.test.shared.data.TestFixtures
import com.ziemsky.uploader.FileRepository
import com.ziemsky.uploader.Janitor
import com.ziemsky.uploader.model.local.LocalFile
import com.ziemsky.uploader.test.integration.config.IntegrationTestConfig
import io.kotlintest.Spec
import io.kotlintest.shouldBe
import io.kotlintest.specs.BehaviorSpec
import io.mockk.mockk
import org.springframework.test.context.ContextConfiguration
import java.io.File
import java.nio.file.Path

@ContextConfiguration(classes = [(IntegrationTestConfig::class)])
class JanitorSpec(testFixtures: TestFixtures,
                  testDirectory: Path
) : BehaviorSpec() {



    private lateinit var janitor: Janitor

    override fun beforeSpec(spec: Spec) {
        janitor = Janitor(
                mockk<FileRepository>(), // unused in these tests
                maxDailyFoldersCount = 5
        )
    }

    init {

        given("Existing local files, some to keep, one to cleanup") {

            testFixtures.cleanupLocalTestDir()


            // todo get file from fle? can be tricky, since the actual path only comes to play on FsStructure.saveIn
            val localFileToCleanUp = LocalFile(File(testDirectory.toString(), "20180908120000-01-front.jpg"))
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


            `when`("cleaning given local file") {

                // todo consider moving to unit test, delegating localFile.file.delete() to something mockable
                //  and remove testImplementation("io.mockk:mockk") dependency
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
        // todo more test cases - no file? file given but doesn't exist?

    }
}