import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("plugin.spring")

    // Required due to dependency on project :application
    id("org.springframework.boot")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    testImplementation(project(":test-shared-resources"))
    testImplementation(project(":application"))

    testImplementation(files("../lib/fs-structure-0.1.0-SNAPSHOT.jar"))

    testImplementation("io.kotest:kotest-runner-junit5-jvm") {
        // to prevent io.kotest import older kotlin-stdlib-common
        exclude(group = "org.jetbrains.kotlin")
    }
    testImplementation("io.kotest:kotest-runner-console-jvm") {
        // to prevent io.kotest import older kotlin-stdlib-common
        exclude(group = "org.jetbrains.kotlin")
    }
    testImplementation("io.kotest:kotest-extensions-spring") {
        // to prevent io.kotest import older kotlin-stdlib-common
        exclude(group = "org.jetbrains.kotlin")
    }

    testImplementation("org.springframework:spring-test")
    testImplementation("org.springframework:spring-context")

    testImplementation("com.typesafe:config")

    // Google Drive client
    testImplementation("com.google.oauth-client:google-oauth-client-jetty")
    testImplementation("com.google.apis:google-api-services-drive")
    testImplementation("com.google.api-client:google-api-client")

    implementation("io.github.microutils:kotlin-logging") {
        exclude(group = "org.jetbrains.kotlin")
    }
    implementation("ch.qos.logback:logback-classic")

    testRuntimeOnly("com.fasterxml.jackson.core:jackson-databind")

    testImplementation("io.mockk:mockk")
}

val test by tasks.getting(Test::class) {
    useJUnitPlatform()

    systemProperty("conf.path", rootProject.properties["conf.path"] ?: throw MissingConfValueException("conf.path"))
}
repositories {
    mavenCentral()
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "12"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "12"
}

class MissingConfValueException(message: String) : Throwable("Missing configuration value: $message")