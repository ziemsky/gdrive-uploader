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

    override fun toString(): String = value()

    // todo another factory method?
    fun withNextMajorNumber(): ProjectVersion = ProjectVersion(semVer.withNextMajorNumber(), 0, "", repoDirty)
    fun withNextMinorNumber(): ProjectVersion = ProjectVersion(semVer.withNextMinorNumber(), 0, "", repoDirty)
    fun withNextPatchNumber(): ProjectVersion = ProjectVersion(semVer.withNextPatchNumber(), 0, "", repoDirty)

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

        private fun projectVersionFromFullGitVersionTagName(gitVersionTagName: String, isRepoDirty: Boolean): ProjectVersion {

            val projectVersionString = gitVersionTagName.trimStart(*VERSION_TAG_PREFIX.toCharArray())

            val projectVersionStringSegments = projectVersionString.split('-')

            return ProjectVersion(
                    semVer = SemVer.from(projectVersionStringSegments[0]),
                    commitOffset = projectVersionStringSegments[1].toInt(),
                    commitHash = projectVersionStringSegments[2],
                    repoDirty = isRepoDirty
            )
        }

        private fun projectVersionFromGitHash(gitVersionTagName: String, isRepoDirty: Boolean): ProjectVersion {
            return ProjectVersion(
                    semVer = DEFAULT_SEMVER,
                    commitOffset = 0,
                    commitHash = gitVersionTagName,
                    repoDirty = isRepoDirty
            )
        }

        private fun isGitHash(gitVersionTagName: String): Boolean =
                GIT_HASH_REGEX.matches(gitVersionTagName)

        private fun isFullVersion(gitVersionTagName: String): Boolean =
                gitVersionTagName.startsWith(VERSION_TAG_PREFIX)
    }
}