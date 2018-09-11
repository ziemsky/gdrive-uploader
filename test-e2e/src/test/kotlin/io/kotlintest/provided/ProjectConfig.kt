package io.kotlintest.provided

import io.kotlintest.AbstractProjectConfig
import io.kotlintest.extensions.ProjectLevelExtension
import io.kotlintest.spring.SpringAutowireConstructorExtension

object ProjectConfig : AbstractProjectConfig() {

    // Enables integration with Spring DI
    // https://github.com/kotlintest/kotlintest/blob/master/doc/reference.md#constructor-injection
    override fun extensions(): List<ProjectLevelExtension> = listOf(SpringAutowireConstructorExtension)
}