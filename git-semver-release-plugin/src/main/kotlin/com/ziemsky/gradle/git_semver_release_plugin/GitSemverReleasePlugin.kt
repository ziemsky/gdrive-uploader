package com.ziemsky.gradle.git_semver_release_plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionGraphListener
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType

class GitSemverReleasePlugin : Plugin<Project> {

    private lateinit var project: Project
    private lateinit var repo: GitRepo
    private lateinit var logger: Logger

    override fun apply(project: Project) {
        this.project = project
        this.logger = project.logger
        this.repo = GitRepo.at(project.rootDir)

        requireMaxOneReleaseTaskRequested()

        // todo handle version tag on HEAD:  - see requireNoVersionTagOnHead

        initialiseVersionOnRootProject()

        incrementProjectVersionIfRequested()

        registerTasks()

        registerTaskExecutionGraphListener(DependenciesRegisteringTaskExecutionGraphListener())
    }

    private fun registerTaskExecutionGraphListener(vararg taskExecutionGraphListener: TaskExecutionGraphListener) {

        taskExecutionGraphListener.forEach {
            project.rootProject.gradle.taskGraph.addTaskExecutionGraphListener(it)
        }
    }

    private fun registerTasks() {

        project.tasks.register(GitSemverReleaseMajorTask.name, GitSemverReleaseMajorTask::class)

        project.tasks.register(GitSemverReleaseMinorTask.name, GitSemverReleaseMinorTask::class)

        project.tasks.register(GitSemverReleasePatchTask.name, GitSemverReleasePatchTask::class)

        project.tasks.register(GitSemverReleaseDevTask.name, GitSemverReleaseDevTask::class)

        project.tasks.register("versionPrint") {
            doLast {
                reportCurrentProjectVersion()
            }
        }
    }

    private fun incrementProjectVersionIfRequested() {

        val releaseTaskCompanion = requestedReleaseTasksCompanion() ?: GitSemverReleaseDevTask.Companion

        ProjectVersionIncrementer(project, repo).execute(releaseTaskCompanion)
    }

    private fun requestedReleaseTasksCompanion() = requestedReleaseTasksCompanions().singleOrNull()

    private fun requestedReleaseTasksCompanions(): List<GitSemverReleaseTaskCompanion> =
            ALL_RELEASE_TASK_COMPANIONS.filter { isTaskWithNameRequested(it.name) }

    private fun isTaskWithNameRequested(candidateTaskName: String) =
            project.gradle.startParameter.taskNames.contains(candidateTaskName)

    private fun dependsOnTestTasks(task: Task, project: Project) {
        task.dependsOn.add(project.rootProject.tasks.withType<Test>()) // todo move to TaskExecutionGraphListener to register against test tasks of ALL projects - as it stands, only root's tests are invoked
    }

    private fun reportCurrentProjectVersion() = logger.quiet("${project.rootProject.version}")

    private fun initialiseVersionOnRootProject() {
        setProjectVersion(currentGitVersion())
    }

    private fun setProjectVersion(newVersion: ProjectVersion) {

        project.logger.info("Root project's version is $newVersion")

        project.rootProject.version = newVersion
    }

    private fun currentGitVersion(): ProjectVersion {

        val versionTagName = repo.currentVersion(VERSION_TAG_PREFIX)

        return ProjectVersion.from(versionTagName, repo.isDirty())
    }

    private fun requireMaxOneReleaseTaskRequested() {

        val releaseTasksSelectedForExecution = requestedReleaseTasksCompanions()

        val noMoreThanOneReleaseTaskFound = releaseTasksSelectedForExecution.size <= 1

        require(noMoreThanOneReleaseTaskFound) {
            val requestedReleaseTasksNames = releaseTasksSelectedForExecution.map { task -> task.name }.joinToString(", ")

            "At most one release task can be requested at any given time; tasks actually requested: $requestedReleaseTasksNames"
        }
    }

    companion object {

        val VERSION_TAG_PREFIX = "version@"   // todo move?, make configurable

        val ALL_RELEASE_TASK_COMPANIONS = listOf(
                GitSemverReleaseMajorTask.Companion,
                GitSemverReleaseMinorTask.Companion,
                GitSemverReleasePatchTask.Companion,
                GitSemverReleaseDevTask.Companion
        )
    }
}