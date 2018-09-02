package com.ziemsky.gdriveuploader.test.e2e.data

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class TestFixtureService(private val testDirectory: Path) {

    fun createFilesWithDates(vararg testFilesInput:TestFilesInput) {
        testFilesInput.forEach{ testFileInput ->
            for (i in 1..testFileInput.count) {
                Files.createFile(Paths.get(testDirectory.toString(), "${testFileInput.date}_${i}.jpg"))
            }
        }
    }
}

data class TestFilesInput(
        val date: String,
        val count: Int
)