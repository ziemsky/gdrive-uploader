package com.ziemsky.gradle.git_semver_release_plugin

import org.gradle.api.Project
import org.gradle.api.ProjectEvaluationListener
import org.gradle.api.ProjectState
import org.gradle.api.tasks.testing.Test

class DependenciesRegisteringProjectEvaluationListener : ProjectEvaluationListener {

    private fun allTestTasksIn(project: Project) = project.tasks.filterIsInstance<Test>()

    private fun allCheckTasksIn(project: Project) =

            project.rootProject
                    .allprojects
                    .flatMap { project -> project.tasks.filter { it.name == "check" } }
                    .map { task -> task.path }


    private fun allReleaseTasksIn(project: Project) = project.tasks.filterIsInstance<GitSemverReleaseFullTask>()

    override fun afterEvaluate(project: Project, state: ProjectState) {

        println("PROJECT EVALUATED: $project; state: N/A")

        println("RELEASE TASKS: ${allReleaseTasksIn(project.rootProject)}")

        println("TEST TASKS: ${allTestTasksIn(project)}")

        println("CHECK TASKS: ${allCheckTasksIn(project)}")

        allReleaseTasksIn(project.rootProject).forEach { it.dependsOn.addAll(allCheckTasksIn(project)) }


    }

    override fun beforeEvaluate(project: Project) {
//        println("PROJECT EVALUATED: $project; state: N/A")
//
//        println("RELEASE TASKS: ${allReleaseTasksIn(project.rootProject)}")
//
//        println("TEST TASKS: ${allTestTasksIn(project)}")
//
//        println("CHECK TASKS: ${allCheckTasksIn(project)}")
//
//        allReleaseTasksIn(project.rootProject).forEach { it.dependsOn.addAll(allCheckTasksIn(project)) }
    }

}
