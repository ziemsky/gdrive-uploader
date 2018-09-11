package com.ziemsky.uploader

import io.kotlintest.specs.BehaviorSpec
import io.mockk.mockk
import io.mockk.verify
import java.io.File

class SecurerServiceSpec : BehaviorSpec({

    given("A batch of files to secure") {

        val fileRepository: FileRepository = mockk(relaxed = true)

        val files = mockk<List<File>>()

        val securerService = SecurerService(fileRepository)

        `when`("securing files") {

            securerService.secure(files)

            then("secured files the batch in repository") {

                verify { fileRepository.upload(files) }

                verify(exactly = 0) { files.any() }
            }
        }
    }
})