package com.ziemsky.gradle.git_semver_release_plugin

import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.execution.TaskExecutionGraphListener
import org.gradle.api.tasks.testing.Test

class DependenciesRegisteringTaskExecutionGraphListener() : TaskExecutionGraphListener {

    override fun graphPopulated(graph: TaskExecutionGraph) {

        allReleaseTasks(graph).forEach { it.dependsOn.addAll(allTestTasks(graph)) }
    }

    private fun allTestTasks(graph: TaskExecutionGraph) = graph.allTasks.filterIsInstance<Test>()

    private fun allReleaseTasks(graph: TaskExecutionGraph) = graph.allTasks.filterIsInstance<GitSemverReleaseFullTask>()

}
