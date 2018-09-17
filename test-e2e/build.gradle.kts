import com.github.jengelman.gradle.plugins.processes.tasks.JavaFork
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
}

dependencies {
    testImplementation(project(":test-shared-resources"))

    testImplementation(files("../lib/fs-structure-0.1.0-SNAPSHOT.jar"))

    testImplementation("io.kotlintest:kotlintest-runner-junit5:3.1.8")
    testImplementation("io.kotlintest:kotlintest-extensions-spring:3.1.9")

    testImplementation("com.github.tomakehurst:wiremock:2.18.0")

    testImplementation("org.springframework:spring-test:5.0.8.RELEASE")
    testImplementation("org.springframework:spring-web:5.0.8.RELEASE")

    testImplementation("org.awaitility:awaitility-kotlin:3.1.2")

    implementation("io.github.microutils:kotlin-logging:1.6.10")

    testRuntime("com.fasterxml.jackson.core:jackson-databind:2.9.6")
    testRuntime("ch.qos.logback:logback-classic:1.2.3")

    // Google Drive client
    testImplementation("com.google.apis:google-api-services-drive:v3-rev130-1.25.0")
}

val deleteTempTestContent by tasks.registering(Delete::class) {
    delete = setOf("/tmp/inbound") // todo configuration common to the app and tests

    doLast {
        file("/tmp/inbound").mkdirs()
    }
}

tasks.getByPath(":application:appStart").mustRunAfter(deleteTempTestContent) // todo cleanup


val test by tasks.getting(Test::class) {
    useJUnitPlatform()

    dependsOn(tasks.named<Delete>("deleteTempTestContent"))
}
