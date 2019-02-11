package com.ziemsky.uploader

import com.ziemsky.uploader.model.local.LocalFile
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.BehaviorSpec
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeParseException

class LocalFileSpec : BehaviorSpec({

    given("Raw, incoming file with a valid name") {

        val rawIncomingFile = File("20180901120000-00-front.jpg")

        `when`("Transforming the incoming files") {

            val actualLocalFile = LocalFile(rawIncomingFile)

            then("Produces LocalFile object") {

                actualLocalFile shouldNotBe null
                actualLocalFile.date shouldBe LocalDate.of(2018, 9, 1)
            }
        }
    }


    given("Raw, incoming file with invalid name") {

        val rawIncomingFile = File("2018-09-01:12:00:00-00-front.jpg")

        `when`("Transforming the incoming file") {

            then("Reports error") {

                shouldThrow<DateTimeParseException> { // todo custom exception?

                    LocalFile(rawIncomingFile)

                }
            }
        }
    }
})