package com.ziemsky.gradle.git_semver_release_plugin

import org.gradle.api.tasks.TaskAction

open class GitSemverReleaseTask : org.gradle.api.DefaultTask() {

    lateinit var versionSegmentIncrement: (ver: Ver) -> Ver

    @TaskAction
    fun release() {
        val currentProjectVersion = currentProjectVersion()
        println("Tagging HEAD with ${currentProjectVersion.gitVersionTag()}")
        println("Pushing tag ${currentProjectVersion.gitVersionTag()}")

        TODO("tagging and pushing")
    }

    private fun currentProjectVersion(): Ver = project.rootProject.version as Ver
}