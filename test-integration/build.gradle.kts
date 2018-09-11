

plugins {
    kotlin("jvm")
    kotlin("plugin.spring")

    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    testCompile(project(":test-shared-resources"))
    testCompile(project(":application"))

    testCompile("io.kotlintest:kotlintest-runner-junit5:3.1.9")
    testCompile("io.kotlintest:kotlintest-extensions-spring:3.1.9")


    testCompile("com.github.tomakehurst:wiremock:2.18.0")

    testRuntime("com.fasterxml.jackson.core:jackson-databind:2.9.6")
    testRuntime("ch.qos.logback:logback-classic:1.2.3")
}

val test by tasks.getting(Test::class) {
    useJUnitPlatform()
}
