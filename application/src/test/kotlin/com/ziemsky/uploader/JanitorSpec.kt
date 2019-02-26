package com.ziemsky.uploader

import com.ziemsky.uploader.model.local.LocalFile
import com.ziemsky.uploader.model.repo.RepoFolder
import io.kotlintest.TestCase
import io.kotlintest.shouldBe
import io.kotlintest.specs.BehaviorSpec

class JanitorSpec : BehaviorSpec() {

    private var fileRepo = FileRepoStub()

    private var janitor = Janitor(fileRepo, maxDailyFoldersCount = 3)

    override fun beforeTest(testCase: TestCase) {
        fileRepo.resetDailyFolders()
    }

    init {

        given("Remote folders in a number exceeding configured limit") {

            fileRepo.addDailyFolders(
                    "2019-01-01",
                    "2019-02-02",
                    "2019-01-03",
                    "2019-01-04",
                    "2019-01-05"
            )


            `when`("rotating remote folders") {

                janitor.rotateRemoteDailyFolders()

                then("removes oldest daily folders until configured limit is reached") {
                    fileRepo.dailyFolders() shouldBe setOf(
                            "2019-01-01",
                            "2019-02-02",
                            "2019-01-03"
                    )
                }
            }
        }


        given("Remote daily folders in a number matching configured limit") {

            fileRepo.addDailyFolders(
                    "2019-01-01",
                    "2019-02-02",
                    "2019-01-03"
            )

            `when`("rotating remote daily folders") {

                janitor.rotateRemoteDailyFolders()

                then("does not remove anything") {
                    fileRepo.dailyFolders() shouldBe setOf(
                            "2019-01-01",
                            "2019-02-02",
                            "2019-01-03"
                    )
                }
            }
        }


        given("Remote daily folders in a number smaller than the configured limit") {

            fileRepo.addDailyFolders(
                    "2019-01-01",
                    "2019-02-02"
            )

            `when`("rotating remote daily folders") {

                janitor.rotateRemoteDailyFolders()

                then("does not remove anything") {
                    fileRepo.dailyFolders() shouldBe setOf(
                            "2019-01-01",
                            "2019-02-02"
                    )
                }
            }
        }
    }
}

class FileRepoStub : FileRepository {

    private var dailyFolders: MutableSet<String> = mutableSetOf()

    override fun dailyFolderCount(): Int = dailyFolders.size

    fun addDailyFolders(vararg dailyFoldersToAdd: String) {
        dailyFolders.addAll(dailyFoldersToAdd)
    }

    fun resetDailyFolders() = dailyFolders.clear()

    fun dailyFolders(): Set<String> = dailyFolders

    override fun upload(targetFolder: RepoFolder, localFile: LocalFile) {
        // no-op: irrelevant in these tests
    }
}
