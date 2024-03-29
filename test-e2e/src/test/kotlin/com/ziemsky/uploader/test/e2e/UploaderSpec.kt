package com.ziemsky.uploader.test.e2e

import com.ziemsky.fsstructure.FsStructure.*
import com.ziemsky.uploader.test.e2e.config.E2ETestConfig
import com.ziemsky.uploader.test.shared.data.TestFixtures
import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import mu.KotlinLogging
import org.opentest4j.AssertionFailedError
import org.springframework.beans.factory.annotation.Value
import org.springframework.test.context.ContextConfiguration
import java.io.UncheckedIOException
import java.nio.file.Paths
import java.util.*
import kotlin.time.ExperimentalTime
import kotlin.time.minutes

private val log = KotlinLogging.logger {}

private const val maxDailyFolders = 4

/**
 * Main end-to-end test ensuring that all layers integrate together, exercising only core,
 * most critical functionality, leaving testing edge cases to cheaper integration and unit tests.
 */
@OptIn(ExperimentalTime::class)
@ContextConfiguration(classes = [(E2ETestConfig::class)])
// TODO this is now integration test (albeit integrating all layers, rather than just two as the rest of the
//  integration tests in this module), focused on testing that Spring Integration flow is built correctly so
//  move it to test-integration.
//  test-e2e to be used again once building of Docker image gets introduced. At this point,
//  a copy of GDrive client will need to be placed in buildSrc so that it's available to the build script
//  to setup state in GDrive ahead of the test task.
class UploaderSpec(
        @Value("\${conf.path}") private val confPath: String,
        @Value("\${test.e2e.uploader.monitoring.path}") private val monitoringPath: String,
        testFixtures: TestFixtures
) : BehaviorSpec() {

    val rootFolderName = "e2eTest_${UUID.randomUUID()}"

    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf

    private fun startApplication() {
        val args = arrayOf(
                "--spring.config.additional-location=$confPath/application.conf",
                "--uploader.rotation.maxDailyFolders=$maxDailyFolders"
        )
        log.info { "Starting application with args: ${args.joinToString(" ")}" }
        com.ziemsky.uploader.application.main(args)
    }

    init {

        beforeSpec {
            // todo custom Spring Boot Hocon Property Source Starter so that placeholdres can be used in application.conf?
            //  One to:
            //  a) support env vars resolution (ladutsko's doesn't call Config.resolve)
            //  b) support Config.resolveWith(custom config)?
            System.setProperty("uploader.google.drive.tokensDirectory", Paths.get(confPath, "google/gdrive/secrets/tokens").toAbsolutePath().toString())
            System.setProperty("uploader.google.drive.credentialsFile", Paths.get(confPath, "google/gdrive/secrets/credentials.json").toAbsolutePath().toString())
            System.setProperty("uploader.monitoring.path", monitoringPath)
            System.setProperty("uploader.upload.rootFolderName", rootFolderName)
        }

        Given("some remote content and some files in the source dir") {

            // todo elaborate description
            testFixtures.remoteStructureDelete()
            testFixtures.localTestContentDelete() // todo consistent clenup methods' naming
            val rootFolder = testFixtures.createRootFolder(rootFolderName)

            // enough existing daily folders to trigger rotation of the oldest
            val preExistingRemoteContent = create(

                    // matching content
                    dir("2018-09-01", // to be removed during rotation
                            fle("20180901120000-00-front.jpg")
                    ),
                    dir("2018-09-02",
                            fle("20180902120000-00-front.jpg")
                    ),
                    dir("2018-09-03",
                            fle("20180903120000-00-front.jpg")
                    ),

                    // not matching, ignored, remote content to be retained post-rotaion
                    fle("not matching, top level file"),
                    dir("not matching, top-level dir, empty"),
                    dir("not matching, top-level dir, with content",
                            fle("2018090120000-00-front.jpg"), // matching, nested file
                            dir("2018-09-03"), // matching, nested folder
                            dir("not matching, nested dir, empty"),
                            dir("not matching, nested dir, with content",
                                    fle("not matching, nested file")
                            )
                    )
            )
            testFixtures.remoteStructureCreateFrom(rootFolder.id, preExistingRemoteContent)

            // A set of files:
            // - scattered across few, non-consecutive dates,
            // - enough to trigger rotation of the oldest

            val localContentToUpload = create(
                    // 2018-09-03 - files from the day with existing remote folder, to be added to existing folder content
                    fle("20180903120000-01-front.jpg"),

                    // 2018-09-08 - files from day with no remote folder
                    fle("20180908120000-00-front.jpg"),
                    fle("20180908120000-01-front.jpg"),
                    fle("20180908120000-02-front.jpg"),
                    fle("20180908120000-03-front.jpg"),
                    // 2018-09-09 - files from day with no remote folder
                    fle("20180909120000-00-front.jpg"),
                    fle("20180909120000-01-front.jpg"),
                    fle("20180909120000-02-front.jpg"),
                    fle("20180909120000-03-front.jpg"),
                    fle("20180909120000-04-front.jpg"),
                    // 2018-09-11 - files from day with no remote folder
                    fle("20180911120000-00-front.jpg"),
                    fle("20180911120000-01-front.jpg"),
                    fle("20180911120000-02-front.jpg"),
                    fle("20180911120000-03-front.jpg"),
                    fle("20180911120000-04-front.jpg"),
                    fle("20180911120000-05-front.jpg")
            )


            startApplication()

            When("files appear in the monitored location") {

                testFixtures.localStructureCreateFrom(localContentToUpload)
                log.debug { "Created Local Structure" }

                Then("""it uploads all local files
                   |and rotates the remote ones
                   |and deletes local original files
                   |and does not delete any other remote content""".trimMargin()) {
                   // and logs stats?

                    // mix of freshly uploaded files and the remote rotation survivors
                    val remoteContentExpected = create(

                            // actual content: existing ones + the newly uploaded
                            dir("2018-09-03",                           // pre-existing remote folder and content
                                    fle("20180903120000-00-front.jpg"), // pre-existing remote file
                                    fle("20180903120000-01-front.jpg")  // newly uploaded file
                            ),
                            dir("2018-09-08",                           // new remote folder and content
                                    fle("20180908120000-00-front.jpg"),
                                    fle("20180908120000-01-front.jpg"),
                                    fle("20180908120000-02-front.jpg"),
                                    fle("20180908120000-03-front.jpg")
                            ),
                            dir("2018-09-09",                           // new remote folder and content
                                    fle("20180909120000-00-front.jpg"),
                                    fle("20180909120000-01-front.jpg"),
                                    fle("20180909120000-02-front.jpg"),
                                    fle("20180909120000-03-front.jpg"),
                                    fle("20180909120000-04-front.jpg")
                            ),
                            dir("2018-09-11",                           // new remote folder and content
                                    fle("20180911120000-00-front.jpg"),
                                    fle("20180911120000-01-front.jpg"),
                                    fle("20180911120000-02-front.jpg"),
                                    fle("20180911120000-03-front.jpg"),
                                    fle("20180911120000-04-front.jpg"),
                                    fle("20180911120000-05-front.jpg")
                            ),

                            // not matching, ignored, remote content to be retained
                            fle("not matching, top level file"),
                            dir("not matching, top-level dir, empty"),
                            dir("not matching, top-level dir, with content",
                                    fle("2018090120000-00-front.jpg"), // matching, nested file
                                    dir("2018-09-03"), // matching, nested folder
                                    dir("not matching, nested dir, empty"),
                                    dir("not matching, nested dir, with content",
                                            fle("not matching, nested file")
                                    )
                            )
                    )

                    val empty = create()

                    eventually(1.minutes, UncheckedIOException::class) {
                        eventually(1.minutes, AssertionFailedError::class) {
                            testFixtures.localStructure() shouldBe empty // todo FsStructure.EMPTY
                            testFixtures.remoteStructure(rootFolder.id) shouldBe remoteContentExpected
                        }
                    }
                }
            }
        }
    }
}