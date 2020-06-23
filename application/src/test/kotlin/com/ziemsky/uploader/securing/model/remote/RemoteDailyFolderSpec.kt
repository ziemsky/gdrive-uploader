package com.ziemsky.uploader.securing.model.remote

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.LocalDate
import java.time.format.DateTimeParseException

class RemoteDailyFolderSpec : BehaviorSpec({

    Given("Date") {

        val date = LocalDate.of(2019, 2, 16)

        When("Creating folder") {

            val actualRepoFolder = RemoteDailyFolder.from(date)

            Then("Created folder has name derived from the date") {

                actualRepoFolder shouldNotBe null
                actualRepoFolder.name.toString() shouldBe "2019-02-16"
            }
        }
    }

    Given("string representing date in yyyy-MM-dd format") {

        When("Creating folder") {

            val actualRepoFolder = RemoteDailyFolder.from("2019-02-16")

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
                    RemoteDailyFolder.from(invalidFolderName)
                }
            }
        }
    }
})