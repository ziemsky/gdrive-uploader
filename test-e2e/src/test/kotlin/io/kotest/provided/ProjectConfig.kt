package io.kotest.provided

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.extensions.Extension
import io.kotest.extensions.spring.SpringExtension

class ProjectConfig : AbstractProjectConfig() {

    // Enables integration with Spring DI
    // https://kotest.io/docs/extensions/spring.html#constructor-injection
    override fun extensions(): List<Extension> = listOf(SpringExtension)
}