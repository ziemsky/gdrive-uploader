package com.ziemsky.uploader.test.integration.config

import com.google.api.services.drive.Drive
import com.ziemsky.gdriveuploader.test.shared.data.TestFixtureService
import com.ziemsky.gdriveuploader.test.shared.data.TestGDriveProvider
import com.ziemsky.uploader.google.drive.GDriveProvider
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

private val log = KotlinLogging.logger {}

@Configuration
// todo hocon, including, perhaps, getting rid of local/travis_ci vs key env var substitutions in hocon files
//      with env vars containing paths evaluated within Gradle tasks from project.projectDir et.al.
@PropertySource("file:../config/\${uploader.run.environment:local}/test-integration.properties")
class IntegrationTestConfig {

    @Bean
    fun testDirectory(): Path? {
        val testDirectory = Files.createTempDirectory("UploaderIntegrationTest_")
        log.info("Created temporary directory for test content: ${testDirectory}")

        return testDirectory
    }

    @Bean
    fun testData(testDirectory: Path,
                 @Value("\${uploader.google.drive.applicationName}") applicationName: String,
                 @Value("\${uploader.google.drive.applicationUserName}") applicationUserName: String,
                 @Value("\${uploader.google.drive.tokensDirectory}") tokensDirectory: Path,
                 @Value("\${uploader.google.drive.credentialsFile}") credentialsFile: Path
    ): TestFixtureService {

        val drive = TestGDriveProvider( // todo (at least some) literals configurable for different envs.
                applicationUserName,
                tokensDirectory,
                credentialsFile,
                applicationName
        ).drive()


        return TestFixtureService(testDirectory, drive)
    }

    @Bean
    internal fun drive(
            @Value("\${uploader.google.drive.applicationName}") applicationName: String,
            @Value("\${uploader.google.drive.applicationUserName}") applicationUserName: String,
            @Value("\${uploader.google.drive.tokensDirectory}") tokensDirectory: Path,
            @Value("\${uploader.google.drive.credentialsFile}") credentialsFile: Path
    ): Drive {
        return GDriveProvider( // todo (at least some) literals configurable for different envs.
                applicationUserName,
                tokensDirectory,
                credentialsFile,
                applicationName
        ).drive()
    }
}