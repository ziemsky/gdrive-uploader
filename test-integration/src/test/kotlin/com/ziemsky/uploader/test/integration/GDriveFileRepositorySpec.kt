package com.ziemsky.uploader.test.integration

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.ziemsky.gdriveuploader.test.shared.data.TestFilesInput
import com.ziemsky.gdriveuploader.test.shared.data.TestFixtureService
import com.ziemsky.gdriveuploader.test.shared.data.wireMockUtils.verifyEventually
import com.ziemsky.uploader.GDriveFileRepository
import com.ziemsky.uploader.test.integration.config.IntegrationTestConfig
import io.kotlintest.specs.BehaviorSpec
import org.springframework.test.context.ContextConfiguration
import java.io.File

@ContextConfiguration(classes = [(IntegrationTestConfig::class)])
class GDriveFileRepositorySpec(testFixtureService: TestFixtureService): BehaviorSpec({


    given("a single file to secure") {

        testFixtureService.createFilesWithDates(
                TestFilesInput("2018-09-11", 1)
        )

        val filesToSecure: List<File> = listOf(File("testFileOne"))

        val gDriveFileRepository = GDriveFileRepository()

        `when`("securing the file") {

            gDriveFileRepository.upload(filesToSecure)

            then("file is uploaded") {

                verifyEventually(
                        postRequestedFor(urlEqualTo("/upload/drive/v3/files"))
                                .withQueryParam("uploadType", equalTo("media"))
                )
            }
        }
    }
})