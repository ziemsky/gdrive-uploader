package com.ziemsky.uploader.model.repo

import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.BehaviorSpec
import java.time.LocalDate
import java.time.format.DateTimeParseException

class RepoFolderSpec : BehaviorSpec ({

    Given("Date") {

        val date = LocalDate.of(2019, 2, 16)

        When("Creating folder") {

            val actualRepoFolder = RepoFolder.from(date)

            Then("Created folder has name derived from the date") {

                actualRepoFolder shouldNotBe null
                actualRepoFolder.name.toString() shouldBe "2019-02-16"
            }
        }
    }

    Given("string representing date in yyyy-MM-dd format") {

        When("Creating folder") {

            val actualRepoFolder = RepoFolder.from("2019-02-16")

            Then("Created folder has name matching given string") {
                actualRepoFolder shouldNotBe null
                actualRepoFolder.name.toString() shouldBe "2019-02-16"
            }
        }
    }

    Given("string that does not represent date in java.time.format.DateTimeFormatter.ISO_LOCAL_DATE format") {

        val invalidFolderName = "2019-02-16:00"

        When("Creating folder") {

            Then("Error is reported") {

                shouldThrow<DateTimeParseException> { // todo custom exception?
                    RepoFolder.from(invalidFolderName)
                }
            }
        }
    }
})