

plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
}

dependencies {
    testCompile(project(":test-shared-resources"))

    testCompile("io.kotlintest:kotlintest-runner-junit5:3.1.8")
    testCompile("io.kotlintest:kotlintest-extensions-spring:3.1.9")

    testCompile("com.github.tomakehurst:wiremock:2.18.0")

    testCompile("org.springframework:spring-test:5.0.8.RELEASE")
    testCompile("org.springframework:spring-web:5.0.8.RELEASE")

    testCompile("org.awaitility:awaitility-kotlin:3.1.2")

    testRuntime("com.fasterxml.jackson.core:jackson-databind:2.9.6")
    testRuntime("ch.qos.logback:logback-classic:1.2.3")
}

val test by tasks.getting(Test::class) {
    useJUnitPlatform()
}
