package com.ziemsky.gradle.git_semver_release_plugin

import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.execution.TaskExecutionGraphListener
import org.gradle.api.tasks.testing.Test

class DependenciesRegisteringTaskExecutionGraphListener() : TaskExecutionGraphListener {

    override fun graphPopulated(graph: TaskExecutionGraph) {

        allReleaseTasksIn(graph).forEach {
            it.dependsOn.addAll(
                    allCheckTasksIn(graph)
            )
        }

        println("RELEASE TASKS: ${allReleaseTasksIn(graph)}")
        println("TEST TASKS: ${allCheckTasksIn(graph)}")
    }

    private fun allCheckTasksIn(graph: TaskExecutionGraph) = graph.allTasks.filter { it.name == "check" }

    private fun allReleaseTasksIn(graph: TaskExecutionGraph) = graph.allTasks.filterIsInstance<GitSemverReleaseFullTask>()

}
