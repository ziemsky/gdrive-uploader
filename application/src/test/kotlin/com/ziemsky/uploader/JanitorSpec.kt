package com.ziemsky.uploader

import com.ziemsky.uploader.model.local.LocalFile
import com.ziemsky.uploader.model.repo.RepoFolder
import io.kotlintest.IsolationMode
import io.kotlintest.shouldBe
import io.kotlintest.specs.BehaviorSpec
import java.time.LocalDate
import java.util.*

class JanitorSpec : BehaviorSpec() {

    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf

    private var fileRepo = FileRepoStub()

    private var janitor = Janitor(fileRepo, maxDailyFoldersCount = 3)

    init {

        Given("Remote folders in a number exceeding configured limit") {

            fileRepo.resetDailyFolders()
            fileRepo.addDailyFolders(
                    "2019-01-01",
                    "2019-01-02",
                    "2019-01-03",
                    "2019-01-04",
                    "2019-01-05"
            )


            When("rotating remote folders") {

                janitor.rotateRemoteDailyFolders()

                Then("removes oldest daily folders until configured limit is reached") {

                    fileRepo.dailyFolders() shouldBe setOf(
                            "2019-01-03",
                            "2019-01-04",
                            "2019-01-05"
                    )
                }
            }
        }


        Given("Remote daily folders in a number matching configured limit") {

            fileRepo.resetDailyFolders()
            fileRepo.addDailyFolders(
                    "2019-01-01",
                    "2019-01-02",
                    "2019-01-03"
            )

            When("rotating remote daily folders") {

                janitor.rotateRemoteDailyFolders()

                Then("does not remove anything") {

                    fileRepo.dailyFolders() shouldBe setOf(
                            "2019-01-01",
                            "2019-01-02",
                            "2019-01-03"
                    )
                }
            }
        }


        Given("Remote daily folders in a number smaller than the configured limit") {

            fileRepo.resetDailyFolders()
            fileRepo.addDailyFolders(
                    "2019-01-01",
                    "2019-01-02"
            )

            When("rotating remote daily folders") {

                janitor.rotateRemoteDailyFolders()

                Then("does not remove anything") {

                    fileRepo.dailyFolders() shouldBe setOf(
                            "2019-01-01",
                            "2019-01-02"
                    )
                }
            }
        }
    }
}

class FileRepoStub : FileRepository {

    private var dailyFolderNamesSortedAscendingly: SortedSet<String> = sortedSetOf()

    fun addDailyFolders(vararg dailyFoldersToAdd: String) {
        dailyFolderNamesSortedAscendingly.addAll(dailyFoldersToAdd)
    }

    fun resetDailyFolders() {
        dailyFolderNamesSortedAscendingly.clear()
    }

    fun dailyFolders(): SortedSet<String> {
        return dailyFolderNamesSortedAscendingly.toSortedSet()
    }


    override fun dailyFolderCount(): Int {
        return dailyFolderNamesSortedAscendingly.size
    }

    override fun upload(targetFolder: RepoFolder, localFile: LocalFile) {
        // no-op: irrelevant in these tests
    }

    override fun findOldestDailyFolder(): RepoFolder {
        return RepoFolder.from(LocalDate.parse(dailyFolderNamesSortedAscendingly.first()))
    }

    override fun deleteFolder(repoFolder: RepoFolder) {
        dailyFolderNamesSortedAscendingly.remove(repoFolder.name.toString())
    }
}
