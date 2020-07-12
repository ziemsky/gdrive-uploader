package com.ziemsky.gradle.git_semver_release_plugin

import org.gradle.api.Project

class ProjectVersionIncrementer(
        private val project: Project,
        private val repo: GitRepo
) {

    fun execute(releaseSupporter: GitSemverReleaseSupporter) {

        requireReleasePrerequisites(releaseSupporter.releaseTypeSpecificValidators)

        applyNextProjectVersionUsing(releaseSupporter.versionSegmentIncrementer)
    }

    private fun requireReleasePrerequisites(releaseTask: List<(gitRepo: GitRepo) -> Unit>) {
        // todo other preconditions configurable?

        if (honourPreconditionsCheck()) {
            releaseTask.forEach { validator -> validator.invoke(repo) }
        }
    }

    private fun applyNextProjectVersionUsing(versionIncrementer: (projectVersion: ProjectVersion) -> ProjectVersion) {

        val currentProjectVersion = currentProjectVersion()

        val nextProjectVersion = versionIncrementer.invoke(currentProjectVersion)

        setProjectVersion(nextProjectVersion)
    }

    private fun reportProjectVersionIncrement(currentProjectVersion: ProjectVersion, nextProjectVersion: ProjectVersion) {
        project.logger.info("Project version incremented from $currentProjectVersion to $nextProjectVersion")
    }

    private fun currentProjectVersion(): ProjectVersion {
        val rootProjectVersion = project.rootProject.version

        require(rootProjectVersion is ProjectVersion) { "Initialise version on rootProject first." }

        return rootProjectVersion
    }


    private fun setProjectVersion(newProjectVersion: ProjectVersion) {

        val previousProjectVersion = currentProjectVersion()

        if (previousProjectVersion != newProjectVersion) { // can happen for dev releases
            project.rootProject.version = newProjectVersion

            reportProjectVersionIncrement(previousProjectVersion, newProjectVersion)
        }
    }

    private fun honourPreconditionsCheck() = !project.hasProperty("ignorePreconditions")
}