package com.ziemsky.gradle.gitversionreleaseplugin

import java.io.File

class GitRepo private constructor(dir: File) {

    companion object Factory {
        fun of(gitRepoDir: File): GitRepo {
            return GitRepo(gitRepoDir)
        }
    }

}
