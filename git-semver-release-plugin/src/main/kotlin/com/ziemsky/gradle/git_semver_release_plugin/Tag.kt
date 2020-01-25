package com.ziemsky.gradle.git_semver_release_plugin

class Tag private constructor(
        val name: String
) {
    companion object {
        fun from(tagName: String): Tag {

            require(tagName.isNotBlank()) { "Non-empty, non-blank tag name is required; was: '$tagName'" }

            return Tag(tagName)
        }
    }
}