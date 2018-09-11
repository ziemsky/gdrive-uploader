import com.github.jengelman.gradle.plugins.processes.tasks.JavaFork
import org.awaitility.kotlin.await
import org.jetbrains.kotlin.daemon.KotlinCompileDaemon.log
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar

import java.util.concurrent.TimeUnit.*


buildscript {

    repositories {
        mavenCentral()
    }

    dependencies {
        classpath("org.springframework.boot:spring-boot-gradle-plugin:2.0.2.RELEASE")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.2.70")
        classpath("org.jetbrains.kotlin:kotlin-allopen:1.2.70")

        classpath("org.awaitility:awaitility-kotlin:3.1.2")
    }
}

plugins {
    java

    //`kotlin-dsl`
    //https://github.com/gradle/kotlin-dsl/blob/master/doc/getting-started/Configuring-Plugins.md

    kotlin("jvm")
    kotlin("plugin.spring")

    id("org.springframework.boot")
    id("io.spring.dependency-management")

    id("com.github.johnrengelman.processes")
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile(kotlin("reflect"))

    compile("org.springframework.boot:spring-boot-starter-integration")
    compile("org.springframework.integration:spring-integration-file")

    testCompile("org.springframework.boot:spring-boot-starter-test")

    testCompile("io.kotlintest:kotlintest-runner-junit5:3.1.8")

    testCompile("io.mockk:mockk:1.8.7")
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
    baseName = "uploader"
}
tasks.named<BootJar>("bootJar") {
    mustRunAfter(jar)
    baseName = "uploader"
}


val pidFile = File(project.buildDir, "application.pid")

val appStart by tasks.registering(JavaFork::class) {
    group = "Application"
    description = """Starts the application from the assembled JAR file as a background process.
                  |  Use to run the app in the background for e2e tests; for normal run call bootRun task.
                  """.trimMargin()

    main = "-jar"
    args(listOf(
            "build/libs/uploader.jar",
            "--spring.pid.file=${pidFile.path}",
            "--spring.pid.fail-on-write-error=true"
    ))
    // args(listOf(sourceSets["main"].output))

    onlyIf {
        !pidFile.exists()
    }

    doLast {
        println("Looking for PID file ${pidFile.absolutePath}")
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
                      |  Uses command 'kill' which, currently, limits its use to Unix-based systems.
        """.trimMargin()

    executable = "kill"

    onlyIf {
        pidFile.exists()
    }

    doFirst {
        args(listOf(pidFile.readText()))
    }
}