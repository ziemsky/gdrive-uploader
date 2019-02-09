package com.ziemsky.gdriveuploader.test.e2e.config

import java.nio.file.Path
import java.nio.file.Paths

interface TestProperties {
    fun applicationName(): String
    fun applicationUserName(): String
    fun tokensDirectory(): Path
    fun credentialsFile(): Path
}

class MutableTestProperties : TestProperties {

    lateinit var applicationName: String
    lateinit var applicationUserName: String
    lateinit var tokensDirectory: String
    lateinit var credentialsFile: String

    override fun applicationName(): String = applicationName

    override fun applicationUserName(): String = applicationUserName

    override fun tokensDirectory(): Path = Paths.get(tokensDirectory)

    override fun credentialsFile(): Path = Paths.get(credentialsFile)

    override fun toString(): String {
        return "MutableTestProperties(applicationName='$applicationName', applicationUserName='$applicationUserName', tokensDirectory='$tokensDirectory', credentialsFile='$credentialsFile')"
    }
}
