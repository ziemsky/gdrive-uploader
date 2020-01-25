package com.ziemsky.gradle.git_semver_release_plugin

import org.gradle.api.tasks.TaskAction

/**
 * Tags HEAD commit in current git repo with current project's version as annotated tag.
 *
 * It is expected that the version on Project has already been incremented at this point
 * (by [VersionIncrementingReleaseTaskExecutionGraphListener]).
 */
abstract class GitSemverReleaseTask(
        private val versionSegmentIncrementer: (projectVersion: ProjectVersion) -> ProjectVersion,
        private val releaseTypeSpecificValidators: List<(gitRepo: GitRepo) -> Unit>
) : org.gradle.api.DefaultTask() {

    private val repo = GitRepo.at(rootProjectDir)

    protected fun repo() = repo // stops Gradle complaining that property 'is not annotated with an input or output annotation'

    init {
        group = "Release"
    }

    fun releaseTypeSpecificValidators(): List<() -> Unit> = releaseTypeSpecificValidators.map { { it.invoke(repo) } }

    @TaskAction
    fun taskAction() {
        release()
    }

    fun versionSegmentIncrementer(): (ProjectVersion) -> ProjectVersion = this.versionSegmentIncrementer

    protected abstract fun release()

    private val rootProjectDir get() = project.rootProject.projectDir
}

abstract class GitSemverReleaseFullTask(
        versionSegmentIncrementer: (projectVersion: ProjectVersion) -> ProjectVersion
) : GitSemverReleaseTask(
        versionSegmentIncrementer,
        listOf(
                { gitRepo -> requireOnMaster(gitRepo) },
                { gitRepo -> requireCleanRepo(gitRepo) },
                { gitRepo -> requireNoVersionTagOnHead(gitRepo) }
        )
) {
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

        private fun requireOnMaster(gitRepo: GitRepo) = require(isOnMain(gitRepo)) {
            "Cannot release because current branch is not the main one. Checkout the main branch and try again."
        }

        private fun requireCleanRepo(gitRepo: GitRepo) = require(gitRepo.isClean()) {
            "Cannot release because there are uncommitted changes. Commit or stash them and try again."
        }

        private fun requireNoVersionTagOnHead(gitRepo: GitRepo) {} // todo

        // todo make branch configurable but require one and keep master as default // ignore branch is available only in releaseDev
        private fun isOnMain(gitRepo: GitRepo): Boolean = gitRepo.isCurrentBranch("master")
    }
}

open class GitSemverReleaseMajorTask : GitSemverReleaseFullTask(
        { projectVersion: ProjectVersion -> projectVersion.withNextMajorNumber() }
) {
    init {
        description = "Increments major version, tags HEAD commit with it as annotated tag, and pushes the tag to the remote repo, if available."
    }
}

open class GitSemverReleaseMinorTask : GitSemverReleaseFullTask(
        { projectVersion: ProjectVersion -> projectVersion.withNextMinorNumber() }
) {
    init {
        description = "Increments major version, tags HEAD commit with it as annotated tag, and pushes the tag to the remote repo, if available."
    }
}

open class GitSemverReleasePatchTask : GitSemverReleaseFullTask(
        { projectVersion: ProjectVersion -> projectVersion.withNextPatchNumber() }
) {
    init {
        description = "Increments major version, tags HEAD commit with it as annotated tag, and pushes the tag to the remote repo, if available."
    }
}

/**
 * No-op task.
 *
 * Purpose:
 * * search for release tasks by type can find it, in order to trigger
 *   version increment in configuration phase.
 * * consistency (all other release tasks have custom types),
 * * provide extension point for releaseDev-specific features
 *
 * @see [VersionIncrementingReleaseTaskExecutionGraphListener]
 */
open class GitSemverReleaseDevTask : GitSemverReleaseTask(
        { projectVersion: ProjectVersion -> projectVersion },
        emptyList()
) {
    init {
        description = "Releases dev version"
    }

    override fun release() {
        // no-op
    }
}
