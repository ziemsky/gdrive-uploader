package com.ziemsky.gradle.gitversionreleaseplugin

class SemVer private constructor(
        val major: Int,
        val minor: Int,
        val patch: Int
) {
    fun withNextMajorNumber(): SemVer = SemVer(major + 1, 0, 0)
    fun withNextMinorNumber(): SemVer = SemVer(major, minor + 1, 0)
    fun withNextPatchNumber(): SemVer = SemVer(major, minor, patch + 1)

    override fun toString(): String = "$major.$minor.$patch"


    companion object Factory {

        val ZERO = SemVer(0, 0, 0)

        fun from(text: String): SemVer { // todo format check")

            val tagVersionComponents = text.split('.')

            return SemVer(
                    tagVersionComponents[0].toInt(),
                    tagVersionComponents[1].toInt(),
                    tagVersionComponents[2].toInt()
            )
        }
    }
}