package com.ziemsky.uploader.test.integration.config

import com.google.api.services.drive.Drive
import com.typesafe.config.ConfigBeanFactory
import com.typesafe.config.ConfigFactory
import com.ziemsky.gdriveuploader.test.shared.data.TestFixtureService
import com.ziemsky.gdriveuploader.test.shared.data.TestGDriveProvider
import com.ziemsky.uploader.google.drive.GDriveProvider
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.nio.file.Files
import java.nio.file.Path

private val log = KotlinLogging.logger {}

@Configuration
class IntegrationTestConfig {

    @Bean
    fun config(
            @Value("../config/\${uploader.run.environment:local}/test-integration.conf") configFilePath: Path
    ): TestProperties {
        val config = ConfigFactory.parseFile(configFilePath.toFile())

        val testProperties = ConfigBeanFactory.create(config, MutableTestProperties::class.java)

        log.info { "$javaClass: $testProperties" }

        return testProperties
    }

    @Bean
    fun testDirectory(): Path? {
        val testDirectory = Files.createTempDirectory("UploaderIntegrationTest_")
        log.info {"Created temporary directory for test content: $testDirectory" }

        return testDirectory
    }

    @Bean
    fun testData(testDirectory: Path, config: TestProperties): TestFixtureService {

        val drive = TestGDriveProvider(
                config.applicationUserName(),
                config.tokensDirectory(),
                config.credentialsFile(),
                config.applicationName()
        ).drive()

        return TestFixtureService(testDirectory, drive)
    }

    @Bean
    internal fun drive(config: TestProperties): Drive {
        return GDriveProvider(
                config.applicationUserName(),
                config.tokensDirectory(),
                config.credentialsFile(),
                config.applicationName()
        ).drive()
    }
}