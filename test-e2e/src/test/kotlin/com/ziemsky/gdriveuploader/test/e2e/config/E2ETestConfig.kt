package com.ziemsky.gdriveuploader.test.e2e.config

import com.typesafe.config.ConfigBeanFactory
import com.typesafe.config.ConfigFactory
import com.ziemsky.gdriveuploader.test.shared.data.TestFixtureService
import com.ziemsky.gdriveuploader.test.shared.data.TestGDriveProvider
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.nio.file.Path

private val log = KotlinLogging.logger {}

@Configuration
class E2ETestConfig {

    @Bean
    fun config(
            @Value("../config/\${uploader.run.environment:local}/test-e2e.conf") configFilePath: Path
    ): TestProperties {
        val config = ConfigFactory.parseFile(configFilePath.toFile())

        val testProperties = ConfigBeanFactory.create(config, MutableTestProperties::class.java)

        log.info { "$javaClass: $testProperties" }

        return testProperties
    }

    @Bean
    fun testDataService(
            @Value("\${test.e2e.uploader.monitoring.path}") testDirectory: Path,
            config: TestProperties
    ): TestFixtureService {

        log.info { "Using test data from directory: '$testDirectory'" }

        val drive = TestGDriveProvider(
                config.applicationUserName(),
                config.tokensDirectory(),
                config.credentialsFile(),
                config.applicationName()
        ).drive()

        return TestFixtureService(testDirectory, drive)
    }
}
