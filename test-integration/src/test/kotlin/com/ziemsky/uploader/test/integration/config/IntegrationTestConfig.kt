package com.ziemsky.uploader.test.integration.config

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.ziemsky.gdriveuploader.test.shared.data.TestFixtureService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import java.nio.file.Files

@Configuration
@PropertySource("classpath:test-integration.properties")
class IntegrationTestConfig(@Value("\${test.integration.gdrive.port}") val mockGDrivePort: Int) {

    companion object {
        val log = LoggerFactory.getLogger(IntegrationTestConfig::class.java)
    }

    @Bean(destroyMethod = "stop", initMethod = "start")
    fun wireMockServer(): WireMockServer {

        val wireMockServer = WireMockServer(wireMockConfig().port(mockGDrivePort))
        WireMock.configureFor("localhost", mockGDrivePort)
        return wireMockServer
    }

    @Bean
    fun testData(): TestFixtureService {
        val testDirectory = Files.createTempDirectory(IntegrationTestConfig::class.qualifiedName)

        log.info("Created temporary directory for test content: ${testDirectory}")

        return TestFixtureService(testDirectory)
    }
}
