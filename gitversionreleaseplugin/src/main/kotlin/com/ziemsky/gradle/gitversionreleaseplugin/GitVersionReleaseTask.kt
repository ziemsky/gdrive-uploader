package com.ziemsky.gradle.gitversionreleaseplugin

import org.gradle.api.tasks.TaskAction

class GitVersionReleaseTask : org.gradle.api.DefaultTask() {

    @TaskAction
    fun action() {
        println("task action invoked")
    }
}