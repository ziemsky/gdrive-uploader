package com.ziemsky.gradle.gitversionreleaseplugin

import org.eclipse.jgit.api.Git
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

    fun currentVersion(versionTagPrefix: String): Ver = repository { repo ->

        val isRepoDirty = !repo.status().call().isClean

        val versionTagGlobPattern = "$versionTagPrefix*"

        val versionFromGitVanilla = repo
                .describe()                       // https://git-scm.com/docs/git-describe/2.6.7
                .setAlways(true)                  // should there be no tags to derive the version for, display HEAD's hash
                .setTags(true)                    // enables looking for lightweight tags as well as annotated ones (the latter is default)
                .setMatch(versionTagGlobPattern)  // only consider tags matching the pattern: https://linux.die.net/man/7/glob
                .call()

        Ver.from(versionFromGitVanilla, isRepoDirty)
    }


    override fun toString(): String {
        return "GitRepo(dir=$gitRepoDir)"
    }

    private fun <T> repository(function: (Git) -> T): T {
        return Git.open(gitRepoDir.toFile()).use(function)
    }
}
