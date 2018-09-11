package com.ziemsky.gdriveuploader.test.e2e

import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.ziemsky.gdriveuploader.test.e2e.config.E2ETestConfig
import com.ziemsky.gdriveuploader.test.shared.data.TestFilesInput
import com.ziemsky.gdriveuploader.test.shared.data.TestFixtureService
import com.ziemsky.gdriveuploader.test.shared.data.wireMockUtils.verifyEventually
import io.kotlintest.specs.BehaviorSpec
import org.springframework.beans.factory.annotation.Value
import org.springframework.test.context.ContextConfiguration

/**
 * Main end-to-end test ensuring that all layers integrate together, exercising only core,
 * most critical functionality, leaving testing edge cases to cheaper integration and unit tests.
 */
@ContextConfiguration(classes = [(E2ETestConfig::class)])
class UploadSpec(@Value("\${test.e2e.gdrive.port}") val mockGDrivePort: Int,
                 testFixtureService: TestFixtureService
): BehaviorSpec({

    given("files in the source dir") {

        // A set of files:
        // - scattered across few, inconsecutive dates,
        // - with varying numbers of files per day,
        // - with total number large enough to warrant sending in a few batches,
        // - with last batch containing fewer items than the max batch size
        testFixtureService.createFilesWithDates(
                TestFilesInput("2018-09-08", 20),
                TestFilesInput("2018-09-10", 32),
                TestFilesInput("2018-09-11", 5)
        )

        // GDrive to report enough existing daily folders to trigger rotation of the oldest

        val url = "http://localhost:${mockGDrivePort}/upload/drive/v3/files"

//        givenThat(post(urlEqualTo("/upload/drive/v3/files"))
//                .withQueryParam("uploadType", equalTo("multipart"))
//                .willReturn(aResponse()
//                        .withHeader("Content-Type", "application/json")
//                        .withBody("""{"property":"MOCK RESPONSE"}""")
//                )
//        )

        println("URL: ${url}")

        `when`("app is started") {

            then("it uploads all files in batches") {
                verifyEventually(postRequestedFor(urlEqualTo("/upload/drive/v3/files")))
            }
        }
    }


})


// https://www.googleapis.com/drive/v3
// https://developers.google.com/drive/api/v3/reference/

