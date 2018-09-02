import com.github.jengelman.gradle.plugins.processes.tasks.JavaFork
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar


buildscript {

    repositories {
        mavenCentral()
    }

    dependencies {
        classpath("org.springframework.boot:spring-boot-gradle-plugin:2.0.2.RELEASE")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.2.51")
        classpath("org.jetbrains.kotlin:kotlin-allopen:1.2.51")
    }
}

plugins {
    application
    java

    kotlin("jvm") version "1.2.51"

    kotlin("plugin.spring") version "1.2.51"

    id("org.springframework.boot") version "2.0.2.RELEASE"
    id("io.spring.dependency-management") version "1.0.6.RELEASE"

    id("com.github.johnrengelman.processes") version "0.5.0"
}

application {
    mainClassName = "com.ziemsky.uploader.UploaderApplicationKt"
}

//val kotlinCompile: KotlinCompile by tasks
//
//kotlinCompile.kotlinOptions.suppressWarnings = true
//kotlinCompile.kotlinOptions.freeCompilerArgs = arrayListOf("-Xjsr305=strict")
//kotlinCompile.kotlinOptions.jvmTarget = "1.8"

//compileTestKotlin {
//    kotlinOptions {
//        freeCompilerArgs = ["-Xjsr305=strict"]
//        jvmTarget = "1.8"
//    }
//}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = listOf("-Xjsr305=strict")
    }
}

allprojects {
    repositories {
        jcenter()
    }
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile(kotlin("reflect"))

    compile("org.springframework.boot:spring-boot-starter-integration")
    compile("org.springframework.integration:spring-integration-file")

    testCompile("org.springframework.boot:spring-boot-starter-test")
    testCompile("io.kotlintest:kotlintest-runner-junit5:3.1.8")
}

// TASKS

val test by tasks.getting(Test::class) {
    useJUnitPlatform()
}

// tasks from com.github.johnrengelman.processes plugin
val appStart by tasks.registering(JavaFork::class) {
    group = "Application"
    description = """Starts the application from the assembled JAR file as a background process.
                  |  Use to run the app in the background for e2e tests; for normal run call bootRun task.
                  """.trimMargin()

    main = "-jar"
    args(listOf("build/libs/uploader.jar"))
}

val appStop by tasks.registering(Exec::class) {
    group = "Application"
    description = """Kills process identified by PID found in file application.pid.
                      |  Uses command 'kill' which, currently, limits its use to Unix-based systems.
        """.trimMargin()

    executable = "kill"

    val pidFile = File("application.pid")

    onlyIf {
        pidFile.isFile
    }

    doFirst {
        args(listOf(pidFile.readText()))
    }
}

appStart {dependsOn(tasks.named<BootJar>("bootJar"))}
tasks.getByPath(":e2e:test").dependsOn(appStart).finalizedBy(appStop)