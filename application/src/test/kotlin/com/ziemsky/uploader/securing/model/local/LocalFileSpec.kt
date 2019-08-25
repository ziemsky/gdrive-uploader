package com.ziemsky.uploader.securing.model.local

import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.nio.file.Paths
import java.time.LocalDate
import java.time.format.DateTimeParseException

class LocalFileSpec : BehaviorSpec({

    Given("Raw, incoming file with a valid name") {

        val rawIncomingFile:File = mockk()

        val expectedFileName = "20180901120000-00-front.jpg"

        val expectedFilePath = Paths.get("/some/path/", expectedFileName)
        val expectedFileSize: Long = 123456789

        every { rawIncomingFile.name } returns expectedFileName
        every { rawIncomingFile.toPath() } returns expectedFilePath
        every { rawIncomingFile.length() } returns expectedFileSize


        When("Transforming the incoming files") {

            val actualLocalFile = LocalFile(rawIncomingFile)

            Then("Produces LocalFile object") {

                actualLocalFile shouldNotBe null
                actualLocalFile.date shouldBe LocalDate.of(2018, 9, 1)
                actualLocalFile.nameLocal shouldBe LocalFileName(expectedFileName)
                actualLocalFile.path shouldBe expectedFilePath
                actualLocalFile.sizeInBytes shouldBe expectedFileSize
            }
        }
    }


    Given("Raw, incoming file with invalid name") {

        val rawIncomingFile = File("2018-09-01:12:00:00-00-front.jpg")

        When("Transforming the incoming file") {

            Then("Reports error") {

                shouldThrow<DateTimeParseException> { // todo custom exception?

                    LocalFile(rawIncomingFile)

                }
            }
        }
    }

//     todo error when file is a dir?
})