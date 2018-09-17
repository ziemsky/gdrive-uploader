

plugins {
    kotlin("jvm")
    kotlin("plugin.spring")

    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    testImplementation(project(":test-shared-resources"))
    testImplementation(project(":application"))

    testImplementation(files("../lib/fs-structure-0.1.0-SNAPSHOT.jar"))

    testImplementation("io.kotlintest:kotlintest-runner-junit5:3.1.9")
    testImplementation("io.kotlintest:kotlintest-extensions-spring:3.1.9")


    // Google Drive client
    testImplementation("com.google.oauth-client:google-oauth-client-jetty:1.25.0")
    testImplementation("com.google.apis:google-api-services-drive:v3-rev130-1.25.0")
    testImplementation("com.google.api-client:google-api-client:1.25.0")

    implementation("io.github.microutils:kotlin-logging:1.6.10")

    testRuntime("com.fasterxml.jackson.core:jackson-databind:2.9.6")
    testRuntime("ch.qos.logback:logback-classic:1.2.3")
}

val test by tasks.getting(Test::class) {
    useJUnitPlatform()
}
