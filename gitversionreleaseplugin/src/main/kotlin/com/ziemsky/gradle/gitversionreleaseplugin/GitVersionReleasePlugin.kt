package com.ziemsky.gradle.gitversionreleaseplugin

import com.ziemsky.gradle.gitversionreleaseplugin.GitVersionReleasePlugin.Companion.VERSION_TAG_PREFIX
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.withType
import java.io.File

class GitVersionReleasePlugin : Plugin<Project> {

    private lateinit var project: Project
    private lateinit var repo: GitRepo
    private lateinit var logger: Logger

    override fun apply(project: Project) {
        this.project = project
        this.logger = project.logger
        this.repo = GitRepo.at(project.rootDir)

        setCurentGitVersionOnRootProjectOf(project)

        project.task("releaseMajor") {
            doFirst { validateReleasePreconditions(project) }

            doLast {
                println("releasing major version")
            }
        }

        project.task("releaseMinor") {
            doFirst { validateReleasePreconditions(project) }

            doLast {
                println("releasing minor version")
            }
        }

        project.task("releasePatch") {

            doFirst { validateReleasePreconditions(project) }

            dependsOn.add(project.rootProject.tasks.withType<Test>())

            doLast {
                val previousVersion = currentProjectVersion()

                val newVersion = versionWithNextPatchNumber()

                setProjectVersion(project, newVersion)

                tagHeadCommitWith(newVersion)

                try {
                    buildArtefactsWith(newVersion)
                    publishArtefacts()
                    pushHeadTag()

                } catch (e: Exception) {
                    removeTag(newVersion)
                    depublishArtefacts()
                    setProjectVersion(project, previousVersion)
                }
            }
        }

        project.task("versionPrint") {
            doLast {
                logger.quiet("${project.rootProject.version}")
            }
        }
    }

    private fun setProjectVersion(project: Project, newVersion: Ver) {
        project.rootProject.version = newVersion
    }

    private fun reportCurrentProjectVersion() {
        logger.quiet("${project.rootProject.version}")
    }

    private fun setCurentGitVersionOnRootProjectOf(project: Project) {
        setProjectVersion(project, currentGitVersion())
    }

    private fun depublishArtefacts() {
        TODO("Not yet implemented")
    }

    private fun removeTag(tag: Ver) {
        TODO("Not yet implemented")
    }

    private fun pushHeadTag() {
        TODO("Not yet implemented")
    }

    private fun publishArtefacts() {
        TODO("Not yet implemented")
    }

    private fun buildArtefactsWith(version: Ver) {
        TODO("Not yet implemented")
    }

    private fun tagHeadCommitWith(ver: Ver) {
        logger.info("Tagging with ${ver.gitVersionTag()}")

        TODO("Not yet implemented")
    }

    private fun versionWithNextPatchNumber(): Ver = currentProjectVersion().withNextPatchNumber()

    private fun currentProjectVersion(): Ver = project.rootProject.version as Ver

    private fun validateReleasePreconditions(project: Project) {
        if (!project.hasProperty("ignorePreconditions")) { // unofficial option, useful in the plugin's development
            // todo other preconditions configurable?

            validateOnMaster(project)
            validateCleanRepo()
        }
    }

    private fun validateCleanRepo() {
        require(isRepoClean(project)) {
            "This task can only operate when there are no uncommitted changes."
        }
    }

    private fun isRepoClean(project: Project): Boolean {
        TODO("NOT IMPLEMENTED YET")
    }

    private fun validateOnMaster(project: Project) {
        require(onMaster(project)) {
            "This task can only operate when on main branch." // todo make branch configurable
        }

    }

    private fun onMaster(project: Project): Boolean {
        return "master" == currentBranchName(project)
    }

    private fun currentBranchName(project: Project): String {
        return repo(project).use { repo -> repo.branch }
    }

    private fun repo(project: Project): Repository = Git.open(File(project.rootDir, ".git")).repository

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

        val ZERO = Version(0,0,0)

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