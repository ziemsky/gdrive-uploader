package com.ziemsky.gdriveuploader.test.e2e.config

import com.ziemsky.gdriveuploader.test.shared.data.TestFixtureService
import com.ziemsky.gdriveuploader.test.shared.data.TestGDriveProvider
import mu.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import java.nio.file.Paths

private val log = KotlinLogging.logger {}

@Configuration
@PropertySource("classpath:test-e2e.properties")
class E2ETestConfig {

    @Bean
    fun testData(): TestFixtureService {

        val drive = TestGDriveProvider( // todo (at least some) literals conigurable for different envs.
                "uploader",
                "tokens",
                "/credentials.json",
                "Uploader"
        ).drive()

        val testDirectory = Paths.get("/tmp/inbound") // todo configurable + clean after each test

        return TestFixtureService(testDirectory, drive)
    }
}
