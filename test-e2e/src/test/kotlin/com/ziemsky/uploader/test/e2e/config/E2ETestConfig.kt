package com.ziemsky.uploader.test.e2e.config

import com.typesafe.config.ConfigBeanFactory
import com.typesafe.config.ConfigFactory
import com.ziemsky.uploader.test.shared.data.TestFixtures
import com.ziemsky.uploader.test.shared.data.TestGDriveProvider
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.nio.file.Path

private val log = KotlinLogging.logger {}

@Configuration
class E2ETestConfig {

    @Bean
    fun testProperties(
            @Value("\${conf.path}/test-e2e.conf") configFilePath: Path,
            @Value("\${conf.path}") confPath: String
    ): TestProperties {
        // todo cleanup, remove duplication with IntegrationTestConfig
        val config = ConfigFactory
                .parseFile(configFilePath.toFile())
                .resolveWith(
                        ConfigFactory.parseMap(
                                hashMapOf("CONF_PATH" to confPath),
                                "env vars"
                        )
                )

        val testProperties = ConfigBeanFactory.create(config, MutableTestProperties::class.java)

        log.info { "$javaClass: $testProperties" }

        return testProperties
    }

    @Bean
    fun testDataService(
            @Value("\${test.e2e.uploader.monitoring.path}") testDirectory: Path,
            config: TestProperties
    ): TestFixtures {

        log.info { "Using test data from directory: '$testDirectory'" }

        val drive = TestGDriveProvider(
                config.applicationUserName(),
                config.tokensDirectory(),
                config.credentialsFile(),
                config.applicationName()
        ).drive()

        return TestFixtures(testDirectory, drive)
    }
}
