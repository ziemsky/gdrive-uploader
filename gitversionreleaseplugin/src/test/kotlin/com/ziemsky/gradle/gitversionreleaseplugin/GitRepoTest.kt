package com.ziemsky.gradle.gitversionreleaseplugin

import io.kotest.assertions.fail
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

class GitRepoTest : BehaviorSpec() {

    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf

    init {
        Given("some preconditions") {

//            val gitRepo: GitRepo.

            When("an action of the test's subject") {

                Then("expected outcome") {
                    fail("Not implemented, yet.")
                }
            }
        }
    }
}