package com.ziemsky.gradle.gitversionreleaseplugin

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
            doFirst { validateOnMaster(project) }

            doLast {
                println("releasing major version")
            }
        }

        project.task("releaseMinor") {
            doFirst { validateOnMaster(project) }

            doLast {
                println("releasing minor version")
            }
        }

        project.task("releasePatch") {

            doFirst { validateOnMaster(project) }

            dependsOn.add(project.rootProject.tasks.withType<Test>())

            doLast {
                val newVersion = versionWithNextPatchNumber()

                val newVersionTag = Tag.from(newVersion)

                tagHeadCommitWith(newVersionTag)

                try {
                    buildArtefactsWith(newVersion)
                    publishArtefacts()
                    pushHeadTag()

                } catch (e: Exception) {
                    removeTag(newVersionTag)
                    depublishArtefacts()
                }
            }
        }

        project.task("versionPrint") {
            doLast {
                logger.quiet("${project.rootProject.version}")
            }
        }
    }

    private fun reportCurrentProjectVersion() {
        logger.quiet("${project.rootProject.version}")
    }

    private fun setCurentGitVersionOnRootProjectOf(project: Project) {
        project.rootProject.version = currentGitVersion()
    }

    private fun depublishArtefacts() {
        TODO("Not yet implemented")
    }

    private fun removeTag(tag: Tag) {
        TODO("Not yet implemented")
    }

    private fun pushHeadTag() {
        TODO("Not yet implemented")
    }

    private fun publishArtefacts() {
        TODO("Not yet implemented")
    }

    private fun buildArtefactsWith(version: Version) {
        TODO("Not yet implemented")
    }

    private fun tagHeadCommitWith(tag: Tag) {
        logger.info("Tagging with " + tag)

        TODO("Not yet implemented")
    }

    private fun versionWithNextPatchNumber(): Version = latestVersion().withNextPatchNumber()

    private fun latestVersion(): Version = repo.allTagsNames()
            .filter { Tag.isVersion(it) }
            .map { Tag.from(it) }
            .map { Version.from(it) }
            .maxWith(VersionComparator)
            ?: Version.ZERO

    private fun validateOnMaster(project: Project) {

        if (!project.hasProperty("ignoreMaster")) { // unofficial option, useful for development
            require(onMaster(project)) {
                "This task can only operate when on master branch." // todo("make branch configurable")
            }
        }
    }

    private fun onMaster(project: Project): Boolean {
        return "master" == currentBranchName(project)
    }

    private fun currentBranchName(project: Project): String {
        return repo(project).use { repo -> repo.branch }
    }

    private fun repo(project: Project): Repository = Git.open(File(project.rootDir, ".git")).repository

    fun currentGitVersion(): String {
        return repo.currentVersion()
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

object VersionComparator : Comparator<Version> {
    override fun compare(left: Version, right: Version): Int {

        val majorResult = left.major.compareTo(right.major)
        if (majorResult > 0) return 1
        if (majorResult < 0) return -1

        val minorResult = left.minor.compareTo(right.minor)
        if (minorResult > 0) return 1
        if (minorResult < 0) return -1

        val patchResult = left.patch.compareTo(right.patch)
        if (patchResult > 0) return 1
        if (patchResult < 0) return -1

        return 0
    }
}

class Tag private constructor(val value: String) {

    companion object {
        private val versionTagPattern = Regex.fromLiteral("version@\\d+\\.\\d+\\.d+\\")

        fun from(name: String): Tag = Tag(name)

        fun from(version: Version): Tag =
                from("version@${version.major}.${version.minor}.${version.patch}")

        fun isVersion(tagName: String): Boolean = tagName.matches(versionTagPattern)
    }

    override fun toString(): String {
        return "Tag(value='$value')"
    }
}