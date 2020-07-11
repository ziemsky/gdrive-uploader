package com.ziemsky.gradle.git_semver_release_plugin

import org.gradle.api.Project
import org.gradle.api.execution.TaskExecutionGraph

class VersionIncrementer(
        private val project: Project
) {

    fun execute(incrementer: (ProjectVersion) -> ProjectVersion) {

            applyNextProjectVersionUsing(
                    incrementer
            )

    }

    private fun requireReleasePrerequisites(releaseTask: GitSemverReleaseTask) {
        // todo other preconditions configurable?

        if (honourPreconditionsCheck()) {
            releaseTask.releaseTypeSpecificValidators()
                    .forEach { validator -> validator.invoke() }
        }
    }

    private fun applyNextProjectVersionUsing(versionIncrementer: (projectVersion: ProjectVersion) -> ProjectVersion) {

        val currentProjectVersion = currentProjectVersion()

        val nextProjectVersion = versionIncrementer.invoke(currentProjectVersion)

        setProjectVersion(nextProjectVersion)
    }

    private fun reportProjectVersionIncrement(currentProjectVersion: ProjectVersion, nextProjectVersion: ProjectVersion) {
        project.logger.info("Incremented project version from $currentProjectVersion to $nextProjectVersion")
    }

    private fun requestedReleaseTask(releaseTasksSelectedForExecution: List<GitSemverReleaseTask>): GitSemverReleaseTask
            = releaseTasksSelectedForExecution.first()

    private fun currentProjectVersion(): ProjectVersion {

        return if (project.rootProject.version is ProjectVersion) {
            project.rootProject.version as ProjectVersion
        } else {
            ProjectVersion.from("c5e1e2f", false)
        }
    }


    private fun setProjectVersion(newProjectVersion: ProjectVersion) {

        val previousProjectVersion = currentProjectVersion()

        project.rootProject.version = newProjectVersion

        project.logger.quiet("Incrementing project version from $previousProjectVersion to $newProjectVersion")

        reportProjectVersionIncrement(previousProjectVersion, newProjectVersion)
    }

    private fun releaseIsRequested(requestedReleaseTasks: List<GitSemverReleaseTask>): Boolean = !requestedReleaseTasks.isEmpty()

    private fun honourPreconditionsCheck() = !project.hasProperty("ignorePreconditions")

    private fun requireSingleReleaseTaskRegistered(releaseTasksSelectedForExecution: List<GitSemverReleaseTask>) {

        val noMoreThanOneReleaseTaskFound = releaseTasksSelectedForExecution.size < 2

        require (noMoreThanOneReleaseTaskFound) {
            val requestedReleaseTasksNames = releaseTasksSelectedForExecution.map { task -> task.name }.joinToString(", ")

            "At most one release task can be requested at any given time; tasks actually requested: $requestedReleaseTasksNames"
        }
    }

    private fun releaseTasksIn(graph: TaskExecutionGraph) = graph.allTasks.filterIsInstance(GitSemverReleaseTask::class.java)
}