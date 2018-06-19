import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {

    repositories {
        mavenCentral()
    }

    dependencies {
        classpath("org.springframework.boot:spring-boot-gradle-plugin:2.0.2.RELEASE")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.2.41")
        classpath("org.jetbrains.kotlin:kotlin-allopen:1.2.41")
    }
}

plugins {
    application
    kotlin("jvm") version "1.2.41"
    kotlin("plugin.spring") version "1.2.41"

    id("org.springframework.boot") version "2.0.2.RELEASE"
    id("io.spring.dependency-management") version "1.0.5.RELEASE"

}


application {
    mainClassName = "com.ziemsky.uploader.UploaderApplication"
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

repositories {
    jcenter()
}

dependencies {
//    compile(kotlin("stdlib"))
    //compile(kotlin("stdlib-jdk8"))

    compile("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    compile("org.jetbrains.kotlin:kotlin-reflect")

    compile("org.springframework.boot:spring-boot-starter-integration")
    compile("org.springframework.integration:spring-integration-file")

    testCompile("org.springframework.boot:spring-boot-starter-test")
}

