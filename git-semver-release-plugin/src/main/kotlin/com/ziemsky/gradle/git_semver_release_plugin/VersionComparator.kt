package com.ziemsky.gradle.git_semver_release_plugin

object VersionComparator : Comparator<Version> { // todo still needed?
    override fun compare(left: Version, right: Version): Int {

        val majorResult = left.major.compareTo(right.major)
        if (majorResult > 0) return 1
        if (majorResult < 0) return -1

        val minorResult = left.minor.compareTo(right.minor)
        if (minorResult > 0) return 1
        if (minorResult < 0) return -1

        val patchResult = left.patch.compareTo(right.patch)
        if (patchResult > 0) return 1
        if (patchResult < 0) return -1

        return 0
    }
}