package com.ziemsky.gradle.git_semver_release_plugin

class SemVer private constructor(
        val major: Int,
        val minor: Int,
        val patch: Int
) {
    fun withNextMajorNumber(): SemVer = SemVer(major + 1, 0, 0)
    fun withNextMinorNumber(): SemVer = SemVer(major, minor + 1, 0)
    fun withNextPatchNumber(): SemVer = SemVer(major, minor, patch + 1)

    override fun toString(): String = "$major.$minor.$patch"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SemVer

        if (major != other.major) return false
        if (minor != other.minor) return false
        if (patch != other.patch) return false

        return true
    }

    override fun hashCode(): Int {
        var result = major
        result = 31 * result + minor
        result = 31 * result + patch
        return result
    }


    companion object Factory {

        val ZERO = SemVer.from(0, 0, 0)

        fun from(major: Int, minor: Int, patch: Int): SemVer { // todo legal values (>=0) check
            return SemVer(major, minor, patch)
        }

        fun from(text: String): SemVer { // todo format check

            val tagVersionComponents = text.split('.')

            return SemVer(
                    tagVersionComponents[0].toInt(),
                    tagVersionComponents[1].toInt(),
                    tagVersionComponents[2].toInt()
            )
        }
    }
}