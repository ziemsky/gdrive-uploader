package com.ziemsky.gdriveuploader.test.e2e.config

import com.ziemsky.gdriveuploader.test.shared.data.TestFixtureService
import com.ziemsky.gdriveuploader.test.shared.data.TestGDriveProvider
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import java.nio.file.Path

private val log = KotlinLogging.logger {}

@Configuration
@PropertySource("classpath:test-e2e.properties")
class E2ETestConfig {

    @Bean
    fun testDataService(@Value("\${test.e2e.uploader.monitoring.path}") testDirectory: Path): TestFixtureService {

        log.info("Using test data from directory: '$testDirectory'")

        val drive = TestGDriveProvider( // todo credentials configurable for different envs.
                "uploader",
                "tokens",
                "/credentials.json",
                "Uploader"
        ).drive()

        return TestFixtureService(testDirectory, drive)
    }
}
