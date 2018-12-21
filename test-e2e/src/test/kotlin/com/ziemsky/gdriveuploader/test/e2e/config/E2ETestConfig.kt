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
// todo hocon, including, perhaps, getting rid of local/travis_ci vs key env var substitutions in hocon files
//      with env vars containing paths evaluated within Gradle tasks from project.projectDir et.al.
@PropertySource("file:../config/\${uploader.run.environment:local}/test-e2e.properties")
class E2ETestConfig {

    @Bean
    fun testDataService(@Value("\${test.e2e.uploader.monitoring.path}") testDirectory: Path,
                        @Value("\${uploader.google.drive.applicationName}") applicationName: String,
                        @Value("\${uploader.google.drive.applicationUserName}") applicationUserName: String,
                        @Value("\${uploader.google.drive.tokensDirectory}") tokensDirectory: Path,
                        @Value("\${uploader.google.drive.credentialsFile}") credentialsFile: Path
    ): TestFixtureService {

        log.info("Using test data from directory: '$testDirectory'")

        val drive = TestGDriveProvider( // todo credentials configurable for different envs.
                applicationUserName,
                tokensDirectory,
                credentialsFile,
                applicationName
        ).drive()

        return TestFixtureService(testDirectory, drive)
    }
}
