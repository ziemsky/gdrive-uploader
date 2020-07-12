package com.ziemsky.gradle.git_semver_release_plugin

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.execution.TaskExecutionGraphListener
import org.gradle.api.logging.Logger
import org.gradle.kotlin.dsl.register

class GitSemverReleasePlugin : Plugin<Project> {

    private lateinit var rootProject: Project
    private lateinit var repo: GitRepo
    private lateinit var logger: Logger

    override fun apply(project: Project) {
        this.rootProject = project.rootProject
        this.logger = project.logger
        this.repo = GitRepo.at(project.rootDir)

        requireMaxOneReleaseTaskRequested()

        // todo handle version tag on HEAD:  - see requireNoVersionTagOnHead

        initialiseVersionOnRootProject()

        incrementProjectVersionIfRequested()

        registerTasks()

//        registerTaskExecutionGraphListener(
//                DependenciesRegisteringTaskExecutionGraphListener()
//        )

//        rootProject.gradle.addProjectEvaluationListener(DependenciesRegisteringProjectEvaluationListener())
    }

    private fun registerTaskExecutionGraphListener(
            vararg taskExecutionGraphListener: TaskExecutionGraphListener
    ) {
        taskExecutionGraphListener.forEach {
            rootProject.gradle.taskGraph.addTaskExecutionGraphListener(it)
        }
    }

    private fun registerTasks() {

        rootProject.tasks.register(GitSemverReleaseMajorTask.name, GitSemverReleaseMajorTask::class)

        rootProject.tasks.register(GitSemverReleaseMinorTask.name, GitSemverReleaseMinorTask::class)

        rootProject.tasks.register(GitSemverReleasePatchTask.name, GitSemverReleasePatchTask::class)

        rootProject.tasks.register(GitSemverReleaseDevTask.name, GitSemverReleaseDevTask::class)

        rootProject.tasks.register("versionPrint") {
            doLast {
                reportCurrentProjectVersion()
            }
        }
    }

    private fun incrementProjectVersionIfRequested() {

        val releaseTaskCompanion = requestedReleaseTasksCompanion() ?: GitSemverReleaseDevTask.Companion

        ProjectVersionIncrementer(rootProject, repo).execute(releaseTaskCompanion)
    }

    private fun requestedReleaseTasksCompanion() = requestedReleaseTasksCompanions().singleOrNull()

    private fun requestedReleaseTasksCompanions(): List<GitSemverReleaseTaskCompanion> =
            ALL_RELEASE_TASK_COMPANIONS.filter { isTaskWithNameRequested(it.name) }

    private fun isTaskWithNameRequested(candidateTaskName: String) =
            rootProject.gradle.startParameter.taskNames.contains(candidateTaskName)

    private fun reportCurrentProjectVersion() = logger.quiet("${rootProject.version}")

    private fun initialiseVersionOnRootProject() {
        setProjectVersion(currentGitVersion())
    }

    private fun setProjectVersion(newVersion: ProjectVersion) {

        rootProject.logger.info("Root project's version is $newVersion")

        rootProject.version = newVersion
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