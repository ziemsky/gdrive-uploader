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

logger.quiet("PROJECT STATE: ${project.state}")
logger.quiet("HAS TASK releaseMinor: ${project.gradle.startParameter.taskNames}")

        if (project.gradle.startParameter.taskNames.contains("releaseMinor")) {
            VersionIncrementer(project).execute { projectVersion -> projectVersion.withNextMinorNumber() }
        }

//        registerTaskExecutionGraphListener(
//                VersionIncrementingReleaseTaskExecutionGraphListener(project)
//        )

        setCurentGitVersionOnRootProjectOf(project)

        project.tasks.register("releaseMajor", GitSemverReleaseMajorTask::class) {
            dependsOnTestTasks(this, project)
        }

        project.tasks.register("releaseMinor", GitSemverReleaseMinorTask::class) {
            dependsOnTestTasks(this, project)
        }

        project.tasks.register("releasePatch", GitSemverReleasePatchTask::class) {
            dependsOnTestTasks(this, project)
        }

        project.tasks.register("releaseDev", GitSemverReleaseDevTask::class) {
            dependsOnTestTasks(this, project)
        }

        project.task("versionPrint") {
            doLast {
                reportCurrentProjectVersion()
            }
        }
    }

    private fun dependsOnTestTasks(task: Task, project: Project) {
        task.dependsOn.add(project.rootProject.tasks.withType<Test>())
    }

    private fun registerTaskExecutionGraphListener(taskExecutionGraphListener: TaskExecutionGraphListener) {
        project.gradle.taskGraph.addTaskExecutionGraphListener(taskExecutionGraphListener)
    }

    private fun reportCurrentProjectVersion() = logger.quiet("${project.rootProject.version}")

    private fun setCurentGitVersionOnRootProjectOf(project: Project) {
        setProjectVersion(project, currentGitVersion())
    }

    private fun setProjectVersion(project: Project, newVersion: ProjectVersion) {

        project.logger.quiet("Setting rootProject.version to $newVersion")

        project.rootProject.version = newVersion
    }

    fun currentGitVersion(): ProjectVersion {

        val versionTagName = repo.currentVersion(VERSION_TAG_PREFIX)

        return ProjectVersion.from(versionTagName, repo.isDirty())
    }

    companion object {
        val VERSION_TAG_PREFIX = "version@"   // todo move, make configurable
    }
}