import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {

    repositories {
        mavenCentral()
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.2.70")
        classpath("org.jetbrains.kotlin:kotlin-allopen:1.2.70")
    }
}

plugins {
    java

    //`kotlin-dsl`
    //https://github.com/gradle/kotlin-dsl/blob/master/doc/getting-started/Configuring-Plugins.md

    kotlin("jvm") version "1.2.70" apply false
    kotlin("plugin.spring") version "1.2.70" apply false

    id("org.springframework.boot") version "2.0.2.RELEASE" apply false
    id("io.spring.dependency-management") version "1.0.6.RELEASE" apply false

    id("com.github.johnrengelman.processes") version "0.5.0" apply false

    // id("com.dorongold.task-tree") version "1.3"
}

subprojects {
    repositories {
        mavenCentral()
        jcenter()
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
            freeCompilerArgs = listOf("-Xjsr305=strict")
        }
    }

}

//dependencies {
//    compile(kotlin("stdlib-jdk8"))
//    compile(kotlin("reflect"))
//
//    compile("org.springframework.boot:spring-boot-starter-integration")
//    compile("org.springframework.integration:spring-integration-file")
//
//    testCompile("org.springframework.boot:spring-boot-starter-test")
//    testCompile("io.kotlintest:kotlintest-runner-junit5:3.1.8")
//
//    testCompile("io.mockk:mockk:1.8.7")
//}

// TASKS

//val test by tasks.getting(Test::class) {
//    useJUnitPlatform()
//}

tasks.getByPath(":test-e2e:test")
        .dependsOn(tasks.getByPath(":application:appStart"))
        .finalizedBy(tasks.getByPath(":application:appStop"))
