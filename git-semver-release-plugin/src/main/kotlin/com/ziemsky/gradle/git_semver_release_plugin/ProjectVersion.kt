package com.ziemsky.gradle.git_semver_release_plugin

import com.ziemsky.gradle.git_semver_release_plugin.GitSemverReleasePlugin.Companion.VERSION_TAG_PREFIX
import com.ziemsky.gradle.git_semver_release_plugin.SemVer.Factory.ZERO


// project.version according to Gradle docs (https://docs.gradle.org/current/dsl/org.gradle.api.Project.html#org.gradle.api.Project:version)
//   The version of this project. Gradle always uses the toString() value of the version. The version defaults to unspecified.

class ProjectVersion private constructor(
        private val semVer: SemVer,
        private val commitOffset: Int,
        private val commitHash: String,
        private val repoDirty: Boolean
) { // to be set as rootProject.version

    // GitVersionTag: version@0.1.0-18-g89df741-dirty
    // ProjectVersion: 0.1.0-18-g89df741-dirty <- toString: https://docs.gradle.org/current/dsl/org.gradle.api.Project.html#org.gradle.api.Project:version

    fun asGitTagName(): String {
        // todo test
        return "$VERSION_TAG_PREFIX${value()}"
    }

    fun value(): String {

        // todo test and think through - these won't _really_ be used, so hard to tell whether useful

        val commitOffsetFragment = if (commitOffset > 0 || semVer == ZERO) "-$commitOffset" else ""
        val commitHashFragment   = if (commitHash.isNotEmpty()) "-$commitHash" else ""
        val repoDirtyFragment    = if (repoDirty) "-dirty" else ""

        return "$semVer$commitOffsetFragment$commitHashFragment$repoDirtyFragment"
    }

    // todo another factory method?
    fun withNextMajorNumber(): ProjectVersion = ProjectVersion(semVer.withNextMajorNumber(), 0, "", repoDirty)
    fun withNextMinorNumber(): ProjectVersion = ProjectVersion(semVer.withNextMinorNumber(), 0, "", repoDirty)
    fun withNextPatchNumber(): ProjectVersion = ProjectVersion(semVer.withNextPatchNumber(), 0, "", repoDirty)

    override fun toString(): String = value()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProjectVersion

        if (semVer != other.semVer) return false
        if (commitOffset != other.commitOffset) return false
        if (commitHash != other.commitHash) return false
        if (repoDirty != other.repoDirty) return false

        return true
    }

    override fun hashCode(): Int {
        var result = semVer.hashCode()
        result = 31 * result + commitOffset
        result = 31 * result + commitHash.hashCode()
        result = 31 * result + repoDirty.hashCode()
        return result
    }


    companion object {

        private val DEFAULT_SEMVER = ZERO
        private val GIT_HASH_REGEX = Regex("\\b[0-9a-f]{5,40}\\b") // SHA-1

        fun from(gitVersionTagName: String, isRepoDirty: Boolean): ProjectVersion {

            // todo require (not sure if this fits with the 'when' expression below, which came later):
            // when full: offset 0, hash empty, not dirty
            // when dev: offset > 0, hash

            // todo test and address edge cases
            // - when no tag, use ZERO (?)
            // - when HEAD tagged, only prefix and semver will be present (and commit hash: setAlways(true)"
            // - when no tag, only commit hash will be present

            return when {
                isGitHash(gitVersionTagName) -> projectVersionFromGitHash(gitVersionTagName, isRepoDirty)
                isFullVersion(gitVersionTagName) -> projectVersionFromFullGitVersionTagName(gitVersionTagName, isRepoDirty)
                else -> throw IllegalArgumentException("Unsupported gitVersionTagName: '$gitVersionTagName'")
            }
        }

        private fun projectVersionFromFullGitVersionTagName(
                gitVersionTagName: String,
                isRepoDirty: Boolean
        ): ProjectVersion {

            // todo tag format validation

            try {
                val projectVersionString = gitVersionTagName.trimStart(*VERSION_TAG_PREFIX.toCharArray())

                val segments = projectVersionString.split('-')

                return ProjectVersion(
                        semVer = SemVer.from(segments[0]),
                        commitOffset = commitOffsetFrom(segments),
                        commitHash = commitHashFrom(segments),
                        repoDirty = isRepoDirty
                )
            } catch (e: Exception) {
                throw RuntimeException(
                        "Failed to parse project version from git tag: '$gitVersionTagName' for repo that is ${if (isRepoDirty) "dirty" else "not dirty"}.", e
                )
            }
        }

        private fun commitOffsetFrom(segments: List<String>):Int = when (segments.size) {
            3 -> segments[1].toInt() // version@0.10.0-1-g9494b11
            4 -> segments[2].toInt() // version@0.10.0-dirty-1-g9494b11
            else -> 0                // version@0.10.0
        }

        private fun commitHashFrom(segments: List<String>) = if (segments.size > 2) segments[2] else ""

        private fun projectVersionFromGitHash(
                gitVersionTagName: String,
                isRepoDirty: Boolean
        ): ProjectVersion = ProjectVersion(
                semVer = DEFAULT_SEMVER,
                commitOffset = 0,
                commitHash = gitVersionTagName,
                repoDirty = isRepoDirty
        )

        private fun isGitHash(gitVersionTagName: String): Boolean =
                GIT_HASH_REGEX.matches(gitVersionTagName)

        private fun isFullVersion(gitVersionTagName: String): Boolean =
                gitVersionTagName.startsWith(VERSION_TAG_PREFIX)
    }
}
