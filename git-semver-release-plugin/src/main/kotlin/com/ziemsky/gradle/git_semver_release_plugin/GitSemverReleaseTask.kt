package com.ziemsky.gradle.git_semver_release_plugin

import com.ziemsky.gradle.git_semver_release_plugin.GitSemverReleaseFullTask.Companion.NOOP_VERSION_INCREMENTER
import com.ziemsky.gradle.git_semver_release_plugin.GitSemverReleasePlugin.Companion.VERSION_TAG_PREFIX
import org.gradle.api.tasks.TaskAction

/**
 * Tags HEAD commit in current git repo with current project's version as annotated tag.
 *
 * It is expected that the version on Project has already been incremented at this point.
 */
abstract class GitSemverReleaseTask : org.gradle.api.DefaultTask() {

    private val repo = GitRepo.at(rootProjectDir)

    protected fun repo() = repo // stops Gradle complaining that property 'is not annotated with an input or output annotation'

    init {
        group = "Release"
    }

    @TaskAction
    fun taskAction() {
        release()
    }

    protected abstract fun release()

    private val rootProjectDir get() = project.rootProject.projectDir

    companion object {
        val name = "release"
    }
}

abstract class GitSemverReleaseFullTask : GitSemverReleaseTask() {

    override fun release() {
        // This is actually the 'new/next' project version; see class' description.
        val currentProjectVersion = currentProjectVersion()

        val currentProjectVersionTagName = currentProjectVersion.asGitTagName()

        reportProjectVersion(currentProjectVersion)

        repo().tagHeadWithAnnotated(currentProjectVersionTagName)

        if (repo().hasRemote()) repo().pushTag(currentProjectVersionTagName)
    }

    private fun reportProjectVersion(currentProjectVersion: ProjectVersion) {
        project.logger.quiet("New project version: $currentProjectVersion")
        project.logger.quiet("Tagging HEAD commit with tag ${currentProjectVersion.asGitTagName()}")
    }

    private fun currentProjectVersion(): ProjectVersion = project.rootProject.version as ProjectVersion

    companion object {

        val NOOP_VERSION_INCREMENTER = { projectVersion: ProjectVersion -> projectVersion }

        val FULL_RELEASE_TYPE_VALIDATORS:List<(gitRepo: GitRepo) -> Unit> = listOf(
                { gitRepo -> requireOnMaster(gitRepo) },
                { gitRepo -> requireCleanRepo(gitRepo) },
                { gitRepo -> requireNoVersionTagOnHead(gitRepo) }
        )

        private fun requireOnMaster(gitRepo: GitRepo) = require(isOnMain(gitRepo)) {
            "Cannot release because current branch is not the main one. Checkout the main branch and try again."
        }

        private fun requireCleanRepo(gitRepo: GitRepo) = require(gitRepo.isClean()) {
            "Cannot release because there are uncommitted changes. Commit, stash, or revert them, and try again."
        }

        private fun requireNoVersionTagOnHead(gitRepo: GitRepo) = require(noVersionTagOnHeadCommit(gitRepo)) {
            """Cannot release because curent HEAD commit has already been released.
             | If that's not the case, remove '$VERSION_TAG_PREFIX' tag(s) from this commit and try again.""".trimMargin()
        }

        private fun noVersionTagOnHeadCommit(gitRepo: GitRepo): Boolean { // todo

            // https://doc.nuxeo.com/blog/jgit-example/ <-- SEE repo.resolve("HEAD")

            // get all tags, iterate over them, find the ones that point to required commit:
            // https://stackoverflow.com/questions/7501646/jgit-retrieve-tag-associated-with-a-git-commit

            // https://www.baeldung.com/jgit

            // gitRepo.tagNamesOfCommit("HEAD")

            return true;
        }

        // todo make branch configurable but require one and keep master as default // ignore branch is available only in releaseDev
        private fun isOnMain(gitRepo: GitRepo): Boolean = gitRepo.isCurrentBranch("master")
    }
}

open class GitSemverReleaseMajorTask : GitSemverReleaseFullTask() {
    init {
        description = "Increments major version, tags HEAD commit with it as annotated tag, and pushes the tag to the remote repo, if available."
    }

    companion object : GitSemverReleaseTaskCompanion {
        override val name = "releaseMajor"
        override val versionSegmentIncrementer = { projectVersion: ProjectVersion -> projectVersion.withNextMajorNumber() }
        override val releaseTypeSpecificValidators = FULL_RELEASE_TYPE_VALIDATORS
    }
}

open class GitSemverReleaseMinorTask : GitSemverReleaseFullTask() {
    init {
        description = "Increments major version, tags HEAD commit with it as annotated tag, and pushes the tag to the remote repo, if available."
    }

    companion object : GitSemverReleaseTaskCompanion {
        override val name = "releaseMinor"
        override val versionSegmentIncrementer = { projectVersion: ProjectVersion -> projectVersion.withNextMinorNumber() }
        override val releaseTypeSpecificValidators = FULL_RELEASE_TYPE_VALIDATORS
    }
}

open class GitSemverReleasePatchTask : GitSemverReleaseFullTask() {
    init {
        description = "Increments major version, tags HEAD commit with it as annotated tag, and pushes the tag to the remote repo, if available."
    }

    companion object : GitSemverReleaseTaskCompanion {
        override val name = "releasePatch"
        override val versionSegmentIncrementer = { projectVersion: ProjectVersion -> projectVersion.withNextPatchNumber() }
        override val releaseTypeSpecificValidators = FULL_RELEASE_TYPE_VALIDATORS
    }
}

/**
 * No-op task.
 */
open class GitSemverReleaseDevTask : GitSemverReleaseTask() {
    init {
        description = "Releases dev version"
    }

    override fun release() {
        // no-op
    }

    companion object : GitSemverReleaseTaskCompanion {
        override val name = "releaseDev"
        override val versionSegmentIncrementer = NOOP_VERSION_INCREMENTER
        override val releaseTypeSpecificValidators:List<(gitRepo: GitRepo) -> Unit> = emptyList()
    }
}

interface GitSemverReleaseSupporter {

    val versionSegmentIncrementer: (projectVersion: ProjectVersion) -> ProjectVersion

    val releaseTypeSpecificValidators: List<(gitRepo: GitRepo) -> Unit>
}

interface GitSemverReleaseTaskCompanion : GitSemverReleaseSupporter{

    val name: String
}