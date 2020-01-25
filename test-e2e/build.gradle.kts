
plugins {
    kotlin("plugin.spring")

    // Enables resolution of :application's dependencies
    id("org.springframework.boot")
}

dependencies {
    testImplementation(project(":test-shared-resources"))

    // Enables @SpringBootTest(classes = [UploaderConfig::class]) in UploadSpec
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
    testImplementation("org.springframework.boot:spring-boot-test")

    testImplementation("org.awaitility:awaitility-kotlin") {
        exclude(group = "org.jetbrains.kotlin")
    }

    implementation("io.github.microutils:kotlin-logging") {
        exclude(group = "org.jetbrains.kotlin")
        exclude(group = "org.slf4j")
    }
    implementation("ch.qos.logback:logback-classic")

    testImplementation("com.typesafe:config")

    // Google Drive client
    testImplementation("com.google.apis:google-api-services-drive")

    implementation(kotlin("stdlib-jdk8"))
}

// todo move under control of com.ziemsky.uploader.test.e2e.UploaderSpec?
// The app is stateful (caches folders) and needs cycling between e2e tests.
// This means it's pointless to start and stop it just once per testing session, as it needs cycling between tests.
// If that's the case, creation of temp folders could be done from within the test methods and the complexity of the
// below tasks (and setting 'appStartArgs') could be avoided.
val testContentDir = createTempDir("uploader-e2e-test_", ".tmp")

val testContentSetUp by tasks.registering(Task::class) {

    doFirst {
        // set dynamic properties for the application
        rootProject.extra.set("appStartArgs", "--uploader.monitoring.path=$testContentDir")
    }

    doLast {
        file(testContentDir).mkdirs()
    }
}

val testContentTearDown by tasks.registering(Delete::class) {
    delete = setOf(testContentDir)
}

val test by tasks.getting(Test::class) {
    useJUnitPlatform()

    // set dynamic properties for the test code
    systemProperty("test.e2e.uploader.monitoring.path", testContentDir)
    systemProperty("conf.path", rootProject.properties["conf.path"] ?: throw MissingConfValueException("conf.path"))

    dependsOn(
            testContentSetUp.get()
    )

    finalizedBy(
            testContentTearDown.get()
    )
}

class MissingConfValueException(message: String) : Throwable("Missing configuration value: $message")