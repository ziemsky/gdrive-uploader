package com.ziemsky.uploader

import com.ziemsky.uploader.model.local.LocalFile
import com.ziemsky.uploader.model.repo.RepoFolder
import com.ziemsky.uploader.model.repo.RepoFolderName
import io.kotlintest.shouldBe
import java.time.LocalDate
import java.util.*

class JanitorSpec : UploaderAbstractBehaviourSpec() {

    private var fileRepo = RemoteRepoStub()

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

class RemoteRepoStub : RemoteRepository { // todo move to shared test resources?

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

    override fun findOldestDailyFolder(): RepoFolder? {
        return if (dailyFolderNamesSortedAscendingly.isEmpty()) {
            null
        } else {
            RepoFolder.from(LocalDate.parse(dailyFolderNamesSortedAscendingly.first()))
        }
    }

    override fun deleteFolder(repoFolder: RepoFolder) {
        dailyFolderNamesSortedAscendingly.remove(repoFolder.name.toString())
    }

    override fun topLevelFolderWithNameAbsent(folderName: RepoFolderName): Boolean = throw UnsupportedOperationException("not applicable in these tests")

    override fun createFolderWithName(repoFolderName: RepoFolderName): Unit = throw UnsupportedOperationException("not applicable in these tests")
}
