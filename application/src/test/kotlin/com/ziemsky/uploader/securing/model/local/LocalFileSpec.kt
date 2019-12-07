package com.ziemsky.uploader.securing.model.local

import io.kotlintest.assertSoftly
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.specs.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import java.time.Instant
import java.time.LocalDate

class LocalFileSpec : BehaviorSpec({

    Given("Actual filesystem File") {

        val rawIncomingFile:File = mockk()

        val expectedFileName = "someRandomFileName"

        val expectedFilePath = Paths.get("/some/path/", expectedFileName)
        val expectedFileSize: Long = 123456789

        val fileCreationDate = "2018-09-01"
        val basicFileAttributes: BasicFileAttributes = mockk()
        val creationTime: FileTime = FileTime.from(Instant.parse("${fileCreationDate}T12:00:00Z"))

        // We are touching File and Files here - which, normally, wouldn't be mocked and whose use would typically make
        // a test like this an 'integrated' one - but we also need a control over the returned file creation time,
        // which does require mocking (of a static method, in this case - oh, the horror!).
        // For this reason, this test is classed as a unit test, after all.
        mockkStatic(Files::class.java.name)

        every { rawIncomingFile.name } returns expectedFileName
        every { rawIncomingFile.toPath() } returns expectedFilePath
        every { rawIncomingFile.length() } returns expectedFileSize
        every { basicFileAttributes.creationTime() } returns creationTime
        every { Files.readAttributes(expectedFilePath, BasicFileAttributes::class.java) } returns basicFileAttributes


        When("Creating instance of the LocalFile from the raw File") {

            val actualLocalFile = LocalFile(rawIncomingFile)

            Then("Produces fully populated LocalFile instance") {
                assertSoftly {
                    actualLocalFile shouldNotBe null
                    actualLocalFile.date() shouldBe LocalDate.parse(fileCreationDate)
                    actualLocalFile.nameLocal() shouldBe LocalFileName(expectedFileName)
                    actualLocalFile.path() shouldBe expectedFilePath
                    actualLocalFile.sizeInBytes() shouldBe expectedFileSize
                    actualLocalFile.raw() shouldBe rawIncomingFile
                }
            }
        }
    }

    Given("Instance of a LocalFile") {

        val rawFile:File = mockk(relaxed = true)

        val localFile = LocalFile(rawFile)

        When("deleting local file") {

            localFile.delete()

            Then("the deletion is propagated to the raw, underlying File") {
                verify(exactly = 1) { rawFile.delete() }
            }
        }
    }



    // todo error when file is a dir?
    // todo should we test for other error cases? are they even likely, given this is an incoming, actual file?
})