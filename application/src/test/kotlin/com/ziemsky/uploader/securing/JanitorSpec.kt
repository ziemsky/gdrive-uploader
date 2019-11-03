package com.ziemsky.uploader.securing

import com.ziemsky.uploader.UploaderAbstractBehaviourSpec
import com.ziemsky.uploader.securing.model.local.LocalFile
import com.ziemsky.uploader.securing.model.remote.RemoteDailyFolder
import com.ziemsky.uploader.securing.model.remote.RemoteFolderName
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

class RemoteRepoStub : RemoteStorageService { // todo move to shared test resources?

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

    override fun upload(targetDailyFolder: RemoteDailyFolder, localFile: LocalFile) {
        // no-op: irrelevant in these tests
    }

    override fun findOldestDailyFolder(): RemoteDailyFolder? {
        return if (dailyFolderNamesSortedAscendingly.isEmpty()) {
            null
        } else {
            RemoteDailyFolder.from(LocalDate.parse(dailyFolderNamesSortedAscendingly.first()))
        }
    }

    override fun deleteDailyFolder(remoteDailyFolder: RemoteDailyFolder) {
        dailyFolderNamesSortedAscendingly.remove(remoteDailyFolder.name.toString())
    }

    override fun isTopLevelFolderWithNameAbsent(folderName: RemoteFolderName): Boolean = throw UnsupportedOperationException("not applicable in these tests")

    override fun createTopLevelFolder(remoteFolderName: RemoteFolderName): Unit = throw UnsupportedOperationException("not applicable in these tests")
}
