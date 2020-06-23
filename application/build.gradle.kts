import com.bmuschko.gradle.docker.tasks.image.Dockerfile
import org.awaitility.kotlin.await
import org.springframework.boot.gradle.tasks.bundling.BootJar
import java.util.concurrent.TimeUnit.SECONDS


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

    id("com.bmuschko.docker-spring-boot-application")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")

    implementation("org.springframework.boot:spring-boot-starter-integration")
    implementation("org.springframework.integration:spring-integration-file")

    implementation("io.github.microutils:kotlin-logging") {
        exclude(group = "org.jetbrains.kotlin")
    }
    implementation("ch.qos.logback:logback-classic")

    implementation("com.github.ladutsko:spring-boot-starter-hocon")

    implementation("org.hibernate.validator:hibernate-validator")

    implementation("com.jakewharton.byteunits:byteunits")

    implementation("org.apache.commons:commons-lang3")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // Google Drive client
    implementation("com.google.oauth-client:google-oauth-client-jetty")
    implementation("com.google.apis:google-api-services-drive")
    implementation("com.google.api-client:google-api-client")


    testImplementation(project(":test-shared-resources"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")

    testImplementation("io.kotest:kotest-runner-junit5-jvm") {
        // to prevent io.kotest import older kotlin-stdlib-common
        exclude(group = "org.jetbrains.kotlin")
    }
    testImplementation("io.kotest:kotest-property-jvm")

    testImplementation("io.mockk:mockk")
}

// todo document in readme
// https://bmuschko.github.io/gradle-docker-plugin/#getting_started
// https://docs.docker.com/engine/reference/commandline/login/#credentials-store
// https://docs.docker.com/docker-hub/access-tokens/

docker {

    springBootApplication {
        applyPropertyIfProvided("docker.image", images::add)

        jvmArgs.set(listOf("-Xmx32m")) // todo mem/JVM args as env vars + document; mem reqs. may depend on file size

        baseImage.set("openjdk:14-alpine")
    }

    registryCredentials {
        // ideally should use tokens but the plugin does not support them, yet https://docs.docker.com/docker-hub/access-tokens/

        applyPropertyIfProvided("docker.repo.url", url::set)
        applyPropertyIfProvided("docker.repo.user.name", username::set)
        applyPropertyIfProvided("docker.repo.user.pass", password::set)
        applyPropertyIfProvided("docker.repo.user.email", email::set)
    }
}

// TASKS
tasks {


    val test by getting(Test::class) {
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
    val jar by named<Jar>("jar") {
        enabled = true
        archiveBaseName.set("uploader")
    }
    named<BootJar>("bootJar") {
        mustRunAfter(jar)
        archiveBaseName.set("uploader")
    }

    val pidFile = File(project.buildDir, "application.pid")

    register("appStart", Task::class) {
        group = "Application"
        description = """Starts the application from the assembled JAR file as a background process.
                  |  Use to run the app in the background for e2e tests; for normal run call bootRun task.
                  """.trimMargin()

        doFirst {
            val workingDir = rootProject.projectDir // config files specify paths relative to root project's dir

            val runEnvironment = rootProject.findProperty("uploader.run.environment") as String

            val configLocation = "${rootProject.projectDir}/conf/$runEnvironment/application.conf"

            val args = mutableListOf(
                    "java",
                    "-jar",
                    "${project.buildDir}/libs/uploader.jar",
                    "--spring.pid.file=${pidFile.path}",
                    "--spring.pid.fail-on-write-error=true",
                    "--spring.config.additional-location=$configLocation"
            )

            args.addAll(parseAppStartArgs())

            val outputFile = File(project.buildDir,"uploader.log")

            logger.info("Starting process: ${args}")
            val processBuilder = ProcessBuilder(args)
            processBuilder.directory(workingDir)
            processBuilder.redirectOutput(outputFile)
            processBuilder.redirectError(outputFile)

            val process = processBuilder.start()
            logger.info("Process started with PID ${process.pid()}")
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

            logger.info("PID file found for process ${pidFile.readText()}; proceeding.")
        }

        dependsOn(named<BootJar>("bootJar"))
    }

    register("appStop", Exec::class.java) {
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

    named<Dockerfile>("dockerCreateDockerfile") {

        // the plugin creates dir /app and puts all application's components there
        // copies content from application/build/docker - but each component has to be explicitly specified

        runCommand("""
        mkdir /app/config  ; \   
        mkdir /app/log     ; \    
        mkdir /app/inbound 
        """.trimIndent()
        )

        copyFile(
                "config",
                "config"
        )
    }

    val dockerImageCopyCustomContext by registering(Copy::class) {
        // adds custom content to the application/build/docker context dir

        val sourceContextDir = "${rootProject.projectDir}/docker/context"
        val actualContextDir = "${buildDir}/docker"

        from(fileTree(sourceContextDir))
        into(actualContextDir)
    }

    named<Sync>("dockerSyncBuildContext") {
        finalizedBy(dockerImageCopyCustomContext)
    }
}

fun applyPropertyIfProvided(propertyName: String, action: (String) -> Unit) {
    (rootProject.findProperty(propertyName) as String?)?.let(action)
}

fun parseAppStartArgs(): List<String> {
    return ((rootProject.findProperty("appStartArgs") ?: "") as String)
            .split("(?<!\\\\)\\s+".toRegex()) // break down on unescaped white spaces
            .map { it.replace("\\ ", " ") }
}