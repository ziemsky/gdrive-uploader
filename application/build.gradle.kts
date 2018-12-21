import com.github.jengelman.gradle.plugins.processes.tasks.JavaFork
import org.awaitility.kotlin.await
import org.jetbrains.kotlin.daemon.KotlinCompileDaemon.log
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar
import org.springframework.boot.gradle.tasks.run.BootRun

import java.util.concurrent.TimeUnit.*


buildscript {
    val awaitilityVersion: String by rootProject
    dependencies {
        classpath("org.awaitility:awaitility-kotlin:$awaitilityVersion")
    }
}

plugins {
    java

    kotlin("plugin.allopen")
    kotlin("plugin.spring")

    id("org.springframework.boot")

    id("com.github.johnrengelman.processes")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    implementation("org.springframework.boot:spring-boot-starter-integration")
    implementation("org.springframework.integration:spring-integration-file")

    implementation("io.github.microutils:kotlin-logging") {
        exclude(group = "org.jetbrains.kotlin")
    }
    implementation("com.github.ladutsko:spring-boot-starter-hocon")

    implementation("org.hibernate.validator:hibernate-validator")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // Google Drive client
    implementation("com.google.oauth-client:google-oauth-client-jetty")
    implementation("com.google.apis:google-api-services-drive")
    implementation("com.google.api-client:google-api-client")

    testImplementation("org.springframework.boot:spring-boot-starter-test")

    testImplementation("io.kotlintest:kotlintest-runner-junit5")

    testImplementation("io.mockk:mockk")
}

// TASKS

val test by tasks.getting(Test::class) {
    useJUnitPlatform()
}

// Spring Boot plugin disables task 'jar' but other modules depend on this file (look for dependencies
// project(":application"), therefore we need to re-enable its creation.
//
// Also, when 'bootJar' runs, it creates an executable Jar file in the same location and with the same name.
// Rather than renaming one of the files to something else, we're making it 'bootJar' run after 'jar'.
//
// In the scenarios such as ':test-integration:test', only 'application:jar' is executed, providing
// integration test module with compiled application code to test. When a complete executable is needed
// (say for actually running the application) 'bootJar' will be executed, re-packaging and overwriting the
// 'raw' Jar file.
//
// See:
// https://docs.spring.io/spring-boot/docs/current/gradle-plugin/reference/html/#packaging-executable-and-normal
val jar by tasks.named<Jar>("jar") {
    enabled = true
    archiveBaseName.set("uploader")
}
tasks.named<BootJar>("bootJar") {
    mustRunAfter(jar)
    archiveBaseName.set("uploader")
}

val pidFile = File(project.buildDir, "application.pid")

val appStart by tasks.registering(JavaFork::class) {
    group = "Application"
    description = """Starts the application from the assembled JAR file as a background process.
                  |  Use to run the app in the background for e2e tests; for normal run call bootRun task.
                  |
                  """.trimMargin()

    main = "-jar"

    workingDir = rootProject.projectDir // config files specify paths relative to root project's dir

    doFirst {
        val runEnvironment = rootProject.findProperty("uploader.run.environment") as String

        val configLocation = "${rootProject.projectDir}/config/$runEnvironment/"

        val arguments = mutableListOf(
                "${project.buildDir}/libs/uploader.jar",
                "--spring.pid.file=${pidFile.path}",
                "--spring.pid.fail-on-write-error=true",
                "--spring.config.additional-location=$configLocation"
        )

        arguments.addAll(parseAppStartArgs())

        args(arguments)
    }

    onlyIf {
        !pidFile.exists()
    }

    doLast {
        logger.info("Waiting for application to start (waiting for PID file ${pidFile.absolutePath} to show up).")

        await.with()
                .pollDelay(1, SECONDS)
                .pollInterval(1, SECONDS)
                .timeout(10, SECONDS)
                .until({ pidFile.exists() })
    }

    dependsOn(tasks.named<BootJar>("bootJar"))
}

val appStop by tasks.registering(Exec::class) {
    group = "Application"
    description = """Kills process identified by PID found in file application.pid.
                      |  Uses system command 'kill' which, currently, limits its use to Unix-based systems.
        """.trimMargin()

    executable = "kill" // todo see https://github.com/profesorfalken/jProcesses for cross-platform kill

    onlyIf {
        pidFile.exists()
    }

    doFirst {
        args(listOf(pidFile.readText()))
    }
}

fun parseAppStartArgs(): List<String> {
    return ((rootProject.findProperty("appStartArgs") ?: "") as String)
            .split("(?<!\\\\)\\s+".toRegex()) // break down on unescaped white spaces
            .map { it.replace("\\ ", " ") }
}