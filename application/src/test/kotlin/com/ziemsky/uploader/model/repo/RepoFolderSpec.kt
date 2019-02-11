package com.ziemsky.uploader.model.repo

import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.specs.BehaviorSpec
import java.time.LocalDate

class RepoFolderSpec : BehaviorSpec ({

    given("Date") {

        val date = LocalDate.of(2019, 2, 16)

        `when`("Creating folder") {

            val actualRepoFolder = RepoFolder.from(date)

            then("Created folder has name derived from the date") {

                actualRepoFolder shouldNotBe null
                actualRepoFolder.name.toString() shouldBe "2019-02-16"
            }
        }
    }
})