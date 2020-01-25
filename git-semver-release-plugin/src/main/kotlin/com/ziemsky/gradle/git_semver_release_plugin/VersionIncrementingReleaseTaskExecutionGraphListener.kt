package com.ziemsky.gradle.git_semver_release_plugin

import org.gradle.api.Project
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.execution.TaskExecutionGraphListener

/**
 * When it detects that a task of type [GitSemverReleaseTask]
 * was selected for execution, it increments project's version.
 *
 * It increments segment of the version that corresponds to the task.
 *
 * Setting it in response to the task execution graph having been
 * populated, i.e. before any task has actually been executed,
 * the updated version is made available to tasks during their execution.
 *
 * For example, Docker image building task can now apply project's version
 * as image's tag.
 */
class VersionIncrementingReleaseTaskExecutionGraphListener(
        private val project: Project
) : TaskExecutionGraphListener {

    override fun graphPopulated(taskExecutionGraph: TaskExecutionGraph) {

        val releaseTasksSelectedForExecution = releaseTasksIn(taskExecutionGraph)

        if (releaseIsRequested(releaseTasksSelectedForExecution)) {

            requireSingleReleaseTaskRegistered(releaseTasksSelectedForExecution)

            val requestedReleaseTask = requestedReleaseTask(releaseTasksSelectedForExecution)

            requireReleasePrerequisites(requestedReleaseTask)

            applyNextProjectVersionUsing(
                    requestedReleaseTask.versionSegmentIncrementer()
            )
        }
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

    private fun currentProjectVersion(): ProjectVersion = project.rootProject.version as ProjectVersion


    private fun setProjectVersion(newProjectVersion: ProjectVersion) {

        val previousProjectVersion = currentProjectVersion()

        project.rootProject.version = newProjectVersion

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