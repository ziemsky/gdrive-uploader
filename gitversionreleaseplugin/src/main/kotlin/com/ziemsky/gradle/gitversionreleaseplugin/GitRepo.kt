package com.ziemsky.gradle.gitversionreleaseplugin

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants.R_TAGS
import java.io.File
import java.nio.file.Path

class GitRepo private constructor(val gitRepoDir: Path) {

    companion object Factory {

        /**
         * @param gitRepoDir Directory that contains '.git'.
         */
        fun at(gitRepoDir: Path): GitRepo {
            return GitRepo(gitRepoDir)
        }

        /**
         * @param gitRepoDir Directory that contains '.git'.
         */
        fun at(gitRepoDir: File): GitRepo {
            return GitRepo(gitRepoDir.toPath())
        }
    }

    override fun toString(): String {
        return "GitRepo(dir=$gitRepoDir)"
    }

    fun allTagsNames(): Set<String> {
        return repository { git: Git -> git.repository.refDatabase.getRefsByPrefix(R_TAGS) }
                .map { it.name.removePrefix("refs/tags/") }
                .toSet()
    }

    private fun <T> repository(function: (Git) -> T): T {
        return Git.open(gitRepoDir.toFile()).use(function)
    }
}
