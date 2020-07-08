package com.ziemsky.gradle.git_semver_release_plugin

import org.gradle.api.tasks.TaskAction

abstract class GitSemverReleaseTask(
        private val versionSegmentIncrement: (ver: Ver) -> Ver
) : org.gradle.api.DefaultTask() {


    fun versionSegmentIncrement(): (Ver) -> Ver {
        return this.versionSegmentIncrement
    }

    @TaskAction
    fun release() {
        val currentProjectVersion = currentProjectVersion()

        println("Tagging HEAD with ${currentProjectVersion}")
        println("Pushing tag ${currentProjectVersion.gitVersionTag()}")

        TODO("tagging and pushing")
    }

    private fun currentProjectVersion(): Ver = project.rootProject.version as Ver
}

open class GitSemverReleaseMajorTask : GitSemverReleaseTask({ ver: Ver -> ver.withNextMajorNumber() })

open class GitSemverReleaseMinorTask : GitSemverReleaseTask({ ver: Ver -> ver.withNextMinorNumber() })

open class GitSemverReleasePatchTask : GitSemverReleaseTask({ ver: Ver -> ver.withNextPatchNumber() })