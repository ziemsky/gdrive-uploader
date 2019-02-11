package com.ziemsky.gdriveuploader.test.e2e

import com.ziemsky.fsstructure.FsStructure.*
import com.ziemsky.gdriveuploader.test.e2e.config.E2ETestConfig
import com.ziemsky.gdriveuploader.test.shared.data.TestFixtures
import io.kotlintest.eventually
import io.kotlintest.seconds
import io.kotlintest.shouldBe
import io.kotlintest.specs.BehaviorSpec
import org.springframework.test.context.ContextConfiguration

/**
 * Main end-to-end test ensuring that all layers integrate together, exercising only core,
 * most critical functionality, leaving testing edge cases to cheaper integration and unit tests.
 */
@ContextConfiguration(classes = [(E2ETestConfig::class)])
class UploadSpec(testFixtures: TestFixtures) : BehaviorSpec({

    given("some remote content and some files in the source dir") {
        // todo elaborate description
        testFixtures.remoteStructureDelete()

        // enough existing daily folders to trigger rotation of the oldest
        val remoteContentOriginal = create(

                // matching content
                dir("2018-09-01",
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
                        fle("20180904120000-00-front.jpg"), // matching, nested file
                        dir("2018-09-03"), // matching, nested folder
                        dir("not matching, nested dir, empty"),
                        dir("not matching, nested dir, with content",
                                fle("not matching, nested file")
                        )
                )
        )
        testFixtures.remoteStructureCreateFrom(remoteContentOriginal)

        // A set of files:
        // - scattered across few, non-consecutive dates,
        // - enough to trigger rotation of the oldest

        val localContentToUpload = create(
                // 2018-09-08
                fle("20180908120000-00-front.jpg"),
                fle("20180908120000-01-front.jpg"),
                fle("20180908120000-02-front.jpg"),
                fle("20180908120000-03-front.jpg"),
                // 2018-09-09
                fle("20180909120000-00-front.jpg"),
                fle("20180909120000-01-front.jpg"),
                fle("20180909120000-02-front.jpg"),
                fle("20180909120000-03-front.jpg"),
                fle("20180909120000-04-front.jpg"),
                // 2018-09-11
                fle("20180911120000-00-front.jpg"),
                fle("20180911120000-01-front.jpg"),
                fle("20180911120000-02-front.jpg"),
                fle("20180911120000-03-front.jpg"),
                fle("20180911120000-04-front.jpg"),
                fle("20180911120000-05-front.jpg")
        )


        `when`("files appear in the monitored location") {

            testFixtures.localStructureCreateFrom(localContentToUpload)

            then("it uploads all local files and rotates the remote ones") {

                // mix of freshly uploaded files and the remote rotation survivors
                val remoteContentExpected = create(

                        // actual content, result of the newly uploaded files and the remote survivors of the rotation
                        dir("2018-09-03",
                                fle("20180903120000-00-front.jpg")
                        ),
                        dir("2018-09-08",
                                fle("20180908120000-00-front.jpg"),
                                fle("20180908120000-01-front.jpg"),
                                fle("20180908120000-02-front.jpg"),
                                fle("20180908120000-03-front.jpg")
                        ),
                        dir("2018-09-09",
                                fle("20180909120000-00-front.jpg"),
                                fle("20180909120000-01-front.jpg"),
                                fle("20180909120000-02-front.jpg"),
                                fle("20180909120000-03-front.jpg"),
                                fle("20180909120000-04-front.jpg")
                        ),
                        dir("2018-09-11",
                                fle("20180911120000-00-front.jpg"),
                                fle("20180911120000-01-front.jpg"),
                                fle("20180911120000-02-front.jpg"),
                                fle("20180911120000-03-front.jpg"),
                                fle("20180911120000-04-front.jpg"),
                                fle("20180911120000-05-front.jpg")
                        ),

                        // not matching, ignored, remote content to be retained post-rotaion
                        fle("not matching, top level file"),
                        dir("not matching, top-level dir, empty"),
                        dir("not matching, top-level dir, with content",
                                fle("20180904120000-00-front.jpg"), // matching, nested file
                                dir("2018-09-03"), // matching, nested folder
                                dir("not matching, nested dir, empty"),
                                dir("not matching, nested dir, with content",
                                        fle("not matching, nested file")
                                )
                        )
                )

                eventually(120.seconds) {
                    testFixtures.remoteStructure() shouldBe remoteContentExpected
                }
            }
        }
    }


})
