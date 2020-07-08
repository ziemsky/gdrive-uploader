package com.ziemsky.gradle.git_semver_release_plugin

import com.ziemsky.gradle.git_semver_release_plugin.GitSemverReleasePlugin.Companion.VERSION_TAG_PREFIX


// project.version according to Gradle docs (https://docs.gradle.org/current/dsl/org.gradle.api.Project.html#org.gradle.api.Project:version)
//   The version of this project. Gradle always uses the toString() value of the version. The version defaults to unspecified.

class Ver private constructor(
        private val semVer: SemVer,
        private val commitOffset: Int,
        private val commitHash: String,
        private val repoDirty: Boolean
) { // to be set as rootProject.version

    // GitVersionTag: version@0.1.0-18-g89df741-dirty
    // ProjectVersion: 0.1.0-18-g89df741-dirty <- toString: https://docs.gradle.org/current/dsl/org.gradle.api.Project.html#org.gradle.api.Project:version

    fun gitVersionTag(): String {
        // todo test

        return "$VERSION_TAG_PREFIX${projectVersion()}"
    }

    fun projectVersion(): String {

        // todo test and thing through - these won't _really_ be used so hard to tell whether useful
        val commitOffsetFragment = if (commitOffset > 0) "-$commitOffset" else ""
        val commitHashFragment   = if (commitHash.isNotEmpty()) "-$commitHash" else ""
        val repoDirtyFragment    = if (commitOffset > 0 && repoDirty) "-dirty" else ""

        return "$semVer$commitOffsetFragment$commitHashFragment$repoDirtyFragment"
    }

    override fun toString(): String = projectVersion()

    // todo another factory method?
    fun withNextMajorNumber(): Ver = Ver(semVer.withNextMajorNumber(), 0, "", repoDirty)
    fun withNextMinorNumber(): Ver = Ver(semVer.withNextMinorNumber(), 0, "", repoDirty)
    fun withNextPatchNumber(): Ver = Ver(semVer.withNextPatchNumber(), 0, "", repoDirty)

    companion object {
        fun from(gitVersionTag: String, isRepoDirty: Boolean): Ver {

            // todo test and address edge cases
            // - when no tag, use ZERO
            // - when HEAD tagged, only prefix and semver will be present (and commit hash: setAlways(true)"
            // - when no tag, only commit hash will be present

            val projectVer = gitVersionTag.trimStart(*VERSION_TAG_PREFIX.toCharArray())

            val segments = projectVer.split('-')
            val semVer = SemVer.from(segments[0])
            val commitOffset = segments[1].toInt()
            val commitHash = segments[2]

            return Ver(
                    semVer,
                    commitOffset,
                    commitHash,
                    isRepoDirty
            )
        }
    }
}
