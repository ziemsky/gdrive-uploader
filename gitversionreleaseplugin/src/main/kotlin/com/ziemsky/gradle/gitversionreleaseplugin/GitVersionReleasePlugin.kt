package com.ziemsky.gradle.gitversionreleaseplugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.withType
import java.io.File

class GitVersionReleasePlugin : Plugin<Project> {

    private lateinit var project: Project
    private lateinit var repo: GitRepo

    override fun apply(project: Project) {
        this.project = project

        repo = GitRepo.at(project.rootDir)

//      project.rootProject.version = "1.2.3"
//
//      println("ROOT PROJECT VERSION: ${project.rootProject.version}")

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
                println("releasing patch version")
                println("CURRENT BRANCH: ${currentBranchName(project)}")

                val versionWithNextPatchNumber = versionWithNextPatchNumber(project)

                println("versionWithNextPatchNumber: $versionWithNextPatchNumber")

                val tagWithNextPatchNumber = Tag.from(versionWithNextPatchNumber)

                tagHeadCommitWith(versionWithNextPatchNumber)

                try {
                    buildArtefactsWith(versionWithNextPatchNumber)
                    publishArtefacts()
                    pushHeadTag()

                } catch (e: Exception) {
                    removeTag(tagWithNextPatchNumber)
                    depublishArtefacts()
                }
            }
        }
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

    private fun tagHeadCommitWith(version: Version) {
        TODO("Not yet implemented")
    }

    private fun versionWithNextPatchNumber(project: Project): Version {
        val latestVersionTag = latestVersionTag(project)

        return Version.from(latestVersionTag).withNextPatchNumber()
    }

    private fun latestVersionTag(project: Project): Tag {

        // list all tags, sort and find the greatest
//        repo.allTagsNames()
//                .filter { it.startsWith("version@") }
//                .map { Tag.from(it) }
//                .map { Version.from(it) }
//                .sortedBy { version: Version -> version. }

        TODO("Not yet implemented")
    }

    private fun validateOnMaster(project: Project) {

        if (!project.hasProperty("ignoreMaster")) { // unofficial option, useful for development
            require(onMaster(project), {
                "This task can only operate when on master branch." // todo("make branch configurable")
            })
        }
    }

    private fun onMaster(project: Project): Boolean {
        return "master" == currentBranchName(project)
    }

    private fun currentBranchName(project: Project): String {
        return repo(project).use { repo -> repo.branch }
    }

    private fun repo(project: Project): Repository = Git.open(File(project.rootDir, ".git")).repository
}

data class Version(
        val major: Int,
        val minor: Int,
        val patch: Int
) {
    fun withNextMajorNumber(): Version = Version(major + 1, 0, 0)
    fun withNextMinorNumber(): Version = Version(major, minor + 1, 0)
    fun withNextPatchNumber(): Version = Version(major, minor, patch + 1)

    companion object Factory {

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





        return 0;
    }
}

data class Tag(
        val value: String
) {
    companion object Factory {

        fun from(name: String): Tag {
            return Tag(name)
        }

        fun from(version: Version): Tag {
            return from("version@${version.major}.${version.minor}.${version.patch}")
        }
    }
}