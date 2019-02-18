
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

    testImplementation("io.kotlintest:kotlintest-runner-junit5") {
        // to prevent io.kotlintest import older kotlin-stdlib-common
        exclude(group = "org.jetbrains.kotlin")
    }
    testImplementation("io.kotlintest:kotlintest-extensions-spring"){
        // to prevent io.kotlintest import older kotlin-stdlib-common
        exclude(group = "org.jetbrains.kotlin")
    }

    testImplementation("org.springframework:spring-test")
    testImplementation("org.springframework.boot:spring-boot-test")

    testImplementation("org.awaitility:awaitility-kotlin") {
        exclude(group = "org.jetbrains.kotlin")
    }

    implementation("io.github.microutils:kotlin-logging") {
        exclude(group = "org.jetbrains.kotlin")
    }
    implementation("ch.qos.logback:logback-classic")

    testImplementation("com.typesafe:config")

    // Google Drive client
    testImplementation("com.google.apis:google-api-services-drive")

    implementation(kotlin("stdlib-jdk8"))
}

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

tasks.getByPath(":application:appStart").mustRunAfter(testContentSetUp)
testContentTearDown.get().mustRunAfter(":application:appStop")

val test by tasks.getting(Test::class) {
    useJUnitPlatform()

    // set dynamic properties for the test code
    systemProperty("test.e2e.uploader.monitoring.path", testContentDir)
    systemProperty("conf.path", rootProject.properties["conf.path"] ?: throw MissingConfValueException("conf.path"))

    dependsOn(
            testContentSetUp.get()
            // tasks.getByPath(":application:appStart")
    )

    finalizedBy(
            testContentTearDown.get()
            // tasks.getByPath(":application:appStop")
    )
}

class MissingConfValueException(message: String) : Throwable("Missing configuration value: $message")