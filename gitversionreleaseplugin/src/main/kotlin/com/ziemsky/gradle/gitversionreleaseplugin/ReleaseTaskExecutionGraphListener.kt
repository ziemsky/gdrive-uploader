package com.ziemsky.gradle.gitversionreleaseplugin

import org.gradle.api.Project
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.execution.TaskExecutionGraphListener

class ReleaseTaskExecutionGraphListener(
        private val project: Project,
        private val repo: GitRepo
) : TaskExecutionGraphListener {

    override fun graphPopulated(graph: TaskExecutionGraph) {

        if (releaseRequested(graph)) {
            validateReleasePreconditions(graph)

            setNextProjectVersion(firstRequestedReleaseTask(graph).versionSegmentIncrement)
        }
    }

    private fun setNextProjectVersion(versionIncrementer: (ver: Ver) -> Ver) {

        setProjectVersion(
                nextProjectVersionFrom(versionIncrementer)
        )
    }

    private fun nextProjectVersionFrom(versionIncrementer: (ver: Ver) -> Ver): Ver =
            versionIncrementer.invoke(currentProjectVersion())

    private fun firstRequestedReleaseTask(graph: TaskExecutionGraph) = releaseTasksRequested(graph).first()

    private fun currentProjectVersion(): Ver = project.rootProject.version as Ver


    private fun setProjectVersion(newProjectVersion: Ver) {

        val previousProjectVersion = currentProjectVersion()

        project.rootProject.version = newProjectVersion

        project.logger.quiet("Incremented project version from $previousProjectVersion to $newProjectVersion")
    }

    private fun releaseRequested(graph: TaskExecutionGraph): Boolean = !releaseTasksRequested(graph).isEmpty()

    private fun validateReleasePreconditions(graph: TaskExecutionGraph) {

            // todo other preconditions configurable?

        if (honourPreconditionsCheck()) {
            validateOnMaster()
            validateCleanRepo()
            validateSingleReleaseTaskRegistered(graph)
        }
    }

    private fun honourPreconditionsCheck() = !project.hasProperty("ignorePreconditions")

    private fun validateSingleReleaseTaskRegistered(graph: TaskExecutionGraph) {
        val releaseTasksRegisteredForExecution = releaseTasksRequested(graph)

        val noMoreThanOneReleaseTaskFound = releaseTasksRegisteredForExecution.size < 2

        require (noMoreThanOneReleaseTaskFound) {
            val requestedReleaseTasksNames = releaseTasksRegisteredForExecution.map { task -> task.name }.joinToString(", ")

            "At most one release task can be requested at any given time; tasks actually requested: $requestedReleaseTasksNames"
        }
    }

    private fun releaseTasksRequested(graph: TaskExecutionGraph) = graph.allTasks.filterIsInstance(GitVersionReleaseTask::class.java)

    private fun validateCleanRepo() = require(repo.isClean()) {
        "Cannot release because there are uncommitted changes. Commit or stash them and try again."
    }

    private fun validateOnMaster() = require(isOnMain()) {
        "Cannot release because current branch is not the main one. Checkout the main branch and try again."
    }

    private fun isOnMain(): Boolean = "master".equals(repo.currentBranchName(), ignoreCase = true)  // todo make branch configurable
}