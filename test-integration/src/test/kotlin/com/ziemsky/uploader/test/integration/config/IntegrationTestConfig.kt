package com.ziemsky.uploader.test.integration.config

import com.google.api.services.drive.Drive
import com.ziemsky.gdriveuploader.test.shared.data.TestFixtureService
import com.ziemsky.gdriveuploader.test.shared.data.TestGDriveProvider
import com.ziemsky.uploader.google.drive.GDriveProvider
import mu.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import java.nio.file.Files
import java.nio.file.Path

private val log = KotlinLogging.logger {}

@Configuration
@PropertySource("classpath:test-integration.properties")
class IntegrationTestConfig {

    @Bean
    fun testDirectory(): Path? {
        val testDirectory = Files.createTempDirectory("UploaderIntegrationTest_")
        log.info("Created temporary directory for test content: ${testDirectory}")

        return testDirectory
    }

    @Bean
    fun testData(testDirectory: Path): TestFixtureService {

        val drive = TestGDriveProvider( // todo (at least some) literals conigurable for different envs.
                "uploader",
                "tokens",
                "/credentials.json",
                "Uploader"
        ).drive()


        return TestFixtureService(testDirectory, drive)
    }

    @Bean
    internal fun drive(): Drive {
        return GDriveProvider( // todo (at least some) literals conigurable for different envs.
                "uploader",
                "tokens",
                "/credentials.json",
                "Uploader"
        ).drive()
    }
}
