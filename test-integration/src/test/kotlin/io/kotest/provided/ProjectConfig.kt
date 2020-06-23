package io.kotest.provided

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.extensions.Extension
import io.kotest.spring.SpringAutowireConstructorExtension

object ProjectConfig : AbstractProjectConfig() {

    // Enables integration with Spring DI
    // https://github.com/kotest/kotest/blob/master/doc/extensions.md#constructor-injection
    override fun extensions(): List<Extension> = listOf(SpringAutowireConstructorExtension)
}