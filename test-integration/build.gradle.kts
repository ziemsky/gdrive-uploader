import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("plugin.spring")

    // Required due to dependency on project :application
    id("org.springframework.boot")
}

dependencies {
    testImplementation(project(":test-shared-resources"))
    testImplementation(project(":application"))

    testImplementation(files("../lib/fs-structure-0.1.0-SNAPSHOT.jar"))

    testImplementation("io.kotlintest:kotlintest-runner-junit5")
    testImplementation("io.kotlintest:kotlintest-extensions-spring")

    testImplementation("com.typesafe:config")

    // Google Drive client
    testImplementation("com.google.oauth-client:google-oauth-client-jetty")
    testImplementation("com.google.apis:google-api-services-drive")
    testImplementation("com.google.api-client:google-api-client")

    implementation("io.github.microutils:kotlin-logging") {
        exclude(group = "org.jetbrains.kotlin")
    }

    testRuntime("com.fasterxml.jackson.core:jackson-databind")
    implementation(kotlin("stdlib-jdk8"))
}

val test by tasks.getting(Test::class) {
    useJUnitPlatform()
}
repositories {
    mavenCentral()
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}