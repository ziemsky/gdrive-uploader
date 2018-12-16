plugins {
    kotlin("plugin.spring")
}

dependencies {
    testImplementation(project(":test-shared-resources"))

    testImplementation(files("../lib/fs-structure-0.1.0-SNAPSHOT.jar"))

    testImplementation("io.kotlintest:kotlintest-runner-junit5") {
        exclude(group = "org.jetbrains.kotlin")
    }

    testImplementation("io.kotlintest:kotlintest-extensions-spring")

    testImplementation("org.springframework:spring-test")
    testImplementation("org.springframework:spring-web")
    testImplementation("org.springframework:spring-context")

    testImplementation("org.awaitility:awaitility-kotlin") {
        exclude(group = "org.jetbrains.kotlin")
    }

    implementation("io.github.microutils:kotlin-logging") {
        exclude(group = "org.jetbrains.kotlin")
    }

    // Google Drive client
    testImplementation("com.google.apis:google-api-services-drive")
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


    dependsOn(
            testContentSetUp.get(),
            tasks.getByPath(":application:appStart")
    )

    finalizedBy(
            testContentTearDown.get(),
            tasks.getByPath(":application:appStop")
    )
}
