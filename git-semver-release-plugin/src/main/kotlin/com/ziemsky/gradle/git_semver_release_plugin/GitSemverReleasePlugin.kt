package com.ziemsky.gradle.git_semver_release_plugin

import com.ziemsky.gradle.git_semver_release_plugin.GitSemverReleasePlugin.Companion.VERSION_TAG_PREFIX
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.execution.TaskExecutionGraphListener
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType

class GitSemverReleasePlugin : Plugin<Project> {

    private lateinit var project: Project
    private lateinit var repo: GitRepo
    private lateinit var logger: Logger

    override fun apply(project: Project) {
        this.project = project
        this.logger = project.logger
        this.repo = GitRepo.at(project.rootDir)

        register(ReleaseTaskExecutionGraphListener(project, repo))

        setCurentGitVersionOnRootProjectOf(project)

        project.tasks.register("releaseMajor", GitSemverReleaseMajorTask::class) {
            dependsOn.add(project.rootProject.tasks.withType<Test>())
        }

        project.tasks.register("releaseMinor", GitSemverReleaseMinorTask::class) {
            dependsOn.add(project.rootProject.tasks.withType<Test>())
        }

        project.tasks.register("releasePatch", GitSemverReleasePatchTask::class) {
            dependsOn.add(project.rootProject.tasks.withType<Test>())
        }

        project.task("versionPrint") {
            doLast {
                reportCurrentProjectVersion()
            }
        }
    }

    private fun register(taskExecutionGraphListener: TaskExecutionGraphListener) {
        project.gradle.taskGraph.addTaskExecutionGraphListener(taskExecutionGraphListener)
    }

    private fun reportCurrentProjectVersion() = logger.quiet("${project.rootProject.version}")

    private fun setCurentGitVersionOnRootProjectOf(project: Project) {
        setProjectVersion(project, currentGitVersion())
    }

    private fun setProjectVersion(project: Project, newVersion: Ver) {
        project.rootProject.version = newVersion
    }

    private fun tagHeadCommitWith(ver: Ver) {
        logger.info("Tagging with ${ver.gitVersionTag()}")

        TODO("Not yet implemented")
    }

    private fun versionWithNextPatchNumber(): Ver = currentProjectVersion().withNextPatchNumber()

    private fun currentProjectVersion(): Ver = project.rootProject.version as Ver

    fun currentGitVersion(): Ver {
        return repo.currentVersion(VERSION_TAG_PREFIX)
    }

    companion object {
        val VERSION_TAG_PREFIX = "version@"   // todo configurable")
    }
}

class Version private constructor(
        val major: Int,
        val minor: Int,
        val patch: Int
) {
    fun withNextMajorNumber(): Version = Version(major + 1, 0, 0)
    fun withNextMinorNumber(): Version = Version(major, minor + 1, 0)
    fun withNextPatchNumber(): Version = Version(major, minor, patch + 1)

    companion object Factory {

        val ZERO = Version(0, 0, 0)

        fun from(text: String): Version {
            val tagVersionComponents = text.split('.')

            return Version(
                    tagVersionComponents[0].toInt(),
                    tagVersionComponents[1].toInt(),
                    tagVersionComponents[2].toInt()
            )
        }

        fun from(tag: Tag): Version {
            return from(tag.value.split('@').last())
        }
    }

}

class Tag private constructor(val value: String) {

    companion object {
        private val versionTagPattern = Regex.fromLiteral("$VERSION_TAG_PREFIX\\d+\\.\\d+\\.d+\\")

        fun from(name: String): Tag = Tag(name)

        fun from(version: Version): Tag =
                from("$VERSION_TAG_PREFIX${version.major}.${version.minor}.${version.patch}")

        fun isVersion(tagName: String): Boolean = tagName.matches(versionTagPattern)
    }

    override fun toString(): String {
        return "Tag(value='$value')"
    }
}