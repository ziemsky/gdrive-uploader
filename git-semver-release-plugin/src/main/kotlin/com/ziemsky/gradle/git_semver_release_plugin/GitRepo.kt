package com.ziemsky.gradle.git_semver_release_plugin

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.TransportConfigCallback
import org.eclipse.jgit.transport.SshTransport
import org.eclipse.jgit.transport.Transport
import org.eclipse.jgit.transport.JschConfigSessionFactory
import org.eclipse.jgit.transport.OpenSshConfig
import com.jcraft.jsch.Session
import org.eclipse.jgit.lib.Ref

import java.io.File
import java.nio.file.Path

class GitRepo private constructor(val gitRepoDir: Path) {

    companion object {

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
            return at(gitRepoDir.toPath())
        }

        val transportConfigCallback = object: TransportConfigCallback {
            override fun configure(transport: Transport?) {
                (transport as SshTransport).sshSessionFactory = object: JschConfigSessionFactory() {
                    override fun configure(host: OpenSshConfig.Host?, session: Session?) {
                        // no-op: causes using defaults, i.e. looking for private keys in default location
                    }
                }
            }
        }

    }

    fun currentVersion(versionTagPrefix: String): String = repository { repo ->

        val versionTagGlobPattern = "$versionTagPrefix*"

        repo
                .describe()                       // https://git-scm.com/docs/git-describe/2.6.7
                .setAlways(true)                  // should there be no tags to derive the version for, display HEAD's hash
                .setTags(true)                    // enables looking for lightweight tags as well as annotated ones (the latter is default)
                .setMatch(versionTagGlobPattern)  // only tags matching pattern; git uses glob: https://linux.die.net/man/7/glob
                .call()
    }

    override fun toString(): String {
        return "GitRepo(dir=$gitRepoDir)"
    }

    private fun <T> repository(function: (Git) -> T): T {
        return Git.open(gitRepoDir.toFile()).use(function)
    }

    fun isCurrentBranch(branchName: String): Boolean = branchName.equals(currentBranchName())

    private fun currentBranchName(): String = repository { it.repository.branch }

    fun isClean(): Boolean = repository { it.status().call().hasUncommittedChanges() }

    fun isDirty() = !isClean()

    fun tagHeadWithAnnotated(tagName: String) {
        repository { it
                .tag()
                .setAnnotated(true)
                .setName(tagName)
                .call()
        }
    }

    fun pushTag(projectVersionTagName: String) {
        repository { it
                .push()
                .add(projectVersionTagName)
                .setTransportConfigCallback(transportConfigCallback)
                .call()
        }
    }

    fun hasRemote(): Boolean = repository { it
                .repository
                .remoteNames
                .isNotEmpty()
    }


}
