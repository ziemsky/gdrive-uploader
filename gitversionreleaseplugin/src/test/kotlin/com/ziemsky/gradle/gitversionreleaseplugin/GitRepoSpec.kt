package com.ziemsky.gradle.gitversionreleaseplugin

import io.kotest.core.listeners.TestListener
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils.deleteDirectory
import java.nio.file.Files.createTempDirectory
import java.nio.file.Files.exists
import java.nio.file.Path
import java.nio.file.Paths

class GitRepoSpec : BehaviorSpec() {

    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf

    init {
        listener(specTestDirListener)

        Given("""Local Git repo with several commits tagged,
            | mixing lightweight tags with annotated ones
            | where tags are applied on various branches
        """.trimMargin()) {

            val gitRepo: GitRepo = repo("gitRepoWithMixedVersionAndOtherTags")

            When("asked for list of all tags") {

                val allTags = gitRepo.allTagsNames()

                Then("returns all tags") {

                    allTags shouldBe setOf(
                            "v0.0.0",
                            "v0.1.0",
                            "v10.1.0",
                            "version@0.1.0",
                            "version@0.2.0"
                    )
                }
            }

            When("asked for current tag-calculated version") {

                val actualTagCalculatedVersion = gitRepo.currentVersion()

                Then("returns current tag-calculated version") {

                    actualTagCalculatedVersion shouldBe "v0.0.0-1-g5108bac"
                }
            }
        }
    }

    private fun repo(testRepoDirectoryName: String): GitRepo {

        val sourceRepoDir: Path = testDataFile(testRepoDirectoryName)

        val targetRepoDir: Path = Paths.get(
                specTestDirListener.specTestDir.toAbsolutePath().toString(),
                testRepoDirectoryName,
                ".git"
        )

        sourceRepoDir.toFile().copyRecursively(targetRepoDir.toFile())

        return GitRepo.at(targetRepoDir)
    }

    private fun testDataFile(testFileName: String): Path {

        val testFileResourceName = "test-data/${GitRepoSpec::class.simpleName}/$testFileName"

        return GitRepoSpec::class.java.classLoader.getResource(testFileResourceName)
                .let { if (it == null) throw IllegalStateException("Test file $testFileName not found."); it }
                .let { it.toURI() }
                .let { Paths.get(it) }
    }

    object specTestDirListener : TestListener {

        lateinit var specTestDir: Path

        override suspend fun beforeSpec(spec: Spec) {
            specTestDir = withContext(Dispatchers.IO) {
                createTempDirectory(GitRepoSpec::class.qualifiedName + ".")
            }
        }

        override suspend fun afterSpec(spec: Spec) {
            specTestDir.let { dir: Path ->
                withContext(Dispatchers.IO) {
                    if (exists(dir)) {
                        deleteDirectory(dir.toFile())
                    }
                }
            }
        }
    }
}
