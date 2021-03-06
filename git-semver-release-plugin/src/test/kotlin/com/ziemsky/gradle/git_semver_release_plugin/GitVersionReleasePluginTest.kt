package com.ziemsky.gradle.git_semver_release_plugin

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import org.gradle.testfixtures.ProjectBuilder

class GitVersionReleasePluginTest : BehaviorSpec() {

    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf

    init {
        Given("Plugin and Project to apply it to") {

            val project: Project = ProjectBuilder.builder().build()

            When("Plugin is applied") {

                project.pluginManager.apply(GitSemverReleasePlugin::class.java)

                Then("Tasks 'releaseMajor', 'releaseMinor', 'releasePatch', are added to the project") {

                    val tasksNames = project.tasks.withType<GitSemverReleaseTask>().map { it.name }

                    tasksNames shouldContainExactlyInAnyOrder listOf(
                            "releaseMajor",
                            "releaseMinor",
                            "releasePatch"
                    )
                }
            }
        }
    }
}