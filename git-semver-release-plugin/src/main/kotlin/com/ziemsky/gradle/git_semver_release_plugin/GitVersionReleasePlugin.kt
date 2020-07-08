package com.ziemsky.gradle.git_semver_release_plugin

import com.ziemsky.gradle.git_semver_release_plugin.GitVersionReleasePlugin.Companion.VERSION_TAG_PREFIX
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.execution.TaskExecutionGraphListener
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType

class GitVersionReleasePlugin : Plugin<Project> {

    private lateinit var project: Project
    private lateinit var repo: GitRepo
    private lateinit var logger: Logger

    override fun apply(project: Project) {
        this.project = project // todo is this safe? what if applied on sub-projects (new instance of the plugin created)?
        this.logger = project.logger
        this.repo = GitRepo.at(project.rootDir)

        register(ReleaseTaskExecutionGraphListener(project, repo))

        setCurentGitVersionOnRootProjectOf(project)

        // TASKS FOR LOCALLY TRIGGERED RELEASES (1.0)

        // developer simply executes one of these to trigger:
        // - test
        // - artefact assembly (dev has to declare dependsOn.add(assembleArtefacts)
        // - artefact publication (dev has to declare dependsOn.add(publishArtefacts)
        // - version increment
        // - tag with version
        // - push tag with version

        project.tasks.register("releaseMajor", GitSemverReleaseTask::class) {
            versionSegmentIncrement = { ver: Ver -> ver.withNextMajorNumber() }
            dependsOn.add(project.rootProject.tasks.withType<Test>())
        }

        project.tasks.register("releaseMinor", GitSemverReleaseTask::class) {
            versionSegmentIncrement = { ver: Ver -> ver.withNextMinorNumber() }
            dependsOn.add(project.rootProject.tasks.withType<Test>())
        }

        project.tasks.register("releasePatch", GitSemverReleaseTask::class) {
            versionSegmentIncrement = { ver: Ver -> ver.withNextPatchNumber() }
            dependsOn.add(project.rootProject.tasks.withType<Test>())
        }

        // don't
        // Add support for argument that allows incrementing (reject decrement)
        // version of given segment to given value. Normally, increment of higher
        // segment would be called. If jump of several numbers is needed,
        // this can be manually done through git tags. For this super-rare
        // case it's not worth the effort.
        //
        // todo
        // detect 'fast forward' and prevent merging if not fast forward?
        // DOES THIS NEED SUPPORT FOR 'MERGE' TASK/ACTION (action as part of task 'release')?

        // TASKS FOR CI/CD RELEASES (for ver. 2.0)

        // Before raising pull request, developer calls one of tagAsMajorChange, tagAsMinorChange
        // tagAsPatchChange.
        //
        // Merge is triggered on CI/CD server where hook calls 'release' task.
        // 'release' task finds one of the major/minor/patch change and triggers
        // action of corresponding releaseMajor, releaseMinor, releasePatch

        // Set tags on HEAD marking given change as major/minor/patch
        // these will be used by task 'release' to determine which
        // semver component to increment.
        // Calling one task removes tag set by another.
        // project.tasks.register("tagAsMajorChange", GitChangeTagTask::class) {}
        // project.tasks.register("tagAsMinorChange", GitChangeTagTask::class) {}
        // project.tasks.register("tagAsPatchChange", GitChangeTagTask::class) {}

        // Check tag on HEAD: majorChange-hash, minorChange-hash, patchChange-hash
        // depending on the tag, increment corresponding version segment.
        // NOTE: this needs to scan for tag earlier than HEAD on commits
        // NOT INCLUDED in the target branch. Dev may have tagged, raised PR
        // and then pushed additional commits after the PR.
        //
        // Lightweight tags or annotated?
        //
        // project.tasks.register("release", GitVersionReleaseTask::class) {
        //
        //     //
        //     versionSegmentIncrement = { ver: Ver ->
        //
        //
        //
        //         // majorChange-hash -> ver.withNextMajorNumber()
        //         // minorChange-hash -> ver.withNextMinorNumber()
        //         // patchChange-hash -> ver.withNextPatchNumber()
        //
        //         ver
        //     }
        //     dependsOn.add(project.rootProject.tasks.withType<Test>())
        // }





//        project.task("releasePatch") {
//
//            doFirst { validateReleasePreconditions(project) }
//
//            dependsOn.add(project.rootProject.tasks.withType<Test>())
//
//            doLast {
//                val previousVersion = currentProjectVersion()
//
//                val newVersion = versionWithNextPatchNumber()
//
//                setProjectVersion(project, newVersion)
//
//                tagHeadCommitWith(newVersion)
//
//                try {
//                    buildArtefactsWith(newVersion)
//                    publishArtefacts()
//                    pushHeadTag()
//
//                } catch (e: Exception) {
//                    removeTag(newVersion)
//                    depublishArtefacts()
//                    setProjectVersion(project, previousVersion)
//                }
//            }
//        }

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

//
//    private fun depublishArtefacts() {
//        TODO("Not yet implemented")
//    }
//
//    private fun removeTag(tag: Ver) {
//        TODO("Not yet implemented")
//    }
//
//    private fun pushHeadTag() {
//        TODO("Not yet implemented")
//    }
//
//    private fun publishArtefacts() {
//        TODO("Not yet implemented")
//    }
//
//    private fun buildArtefactsWith(version: Ver) {
//        TODO("Not yet implemented")
//    }

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