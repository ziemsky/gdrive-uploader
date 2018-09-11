package com.ziemsky.gdriveuploader.test.shared.data

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class TestFixtureService(private val testDirectory: Path) {

    fun createFilesWithDates(vararg testFilesInput: TestFilesInput): List<File> {

        val createdFiles: MutableList<File> = mutableListOf()

        testFilesInput.forEach { testFileInput ->
            for (i in 1..testFileInput.count) {
                val createdFile = Files.createFile(Paths.get(testDirectory.toString(), "${testFileInput.date}_${i}.jpg"))
                createdFiles.add(createdFile.toFile())
            }
        }

        return createdFiles
    }
}