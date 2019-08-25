package com.ziemsky.uploader.securing.model

import io.kotlintest.IsolationMode
import io.kotlintest.shouldBe
import io.kotlintest.specs.BehaviorSpec
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit.SECONDS

class SecuredFilesBatchStatsTest : BehaviorSpec() {
    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf

    init {
        Given("individual stats values") {

            val expectedFilesCount = 1234
            val expectedStart = Instant.parse("2019-08-21T21:40:00Z")
            val expectedEnd = Instant.parse("2019-08-21T21:43:00Z")
            val expectedTotalFilesSizeInBytes: Long = 10_000

            val expectedDuration = Duration.of(180, SECONDS)
            val expectedSecuredBytesPerSec: Long = 55


            When("creating batch stats object") {

                val actualSecuredFilesBatchStats = SecuredFilesBatchStats(
                        filesCount = expectedFilesCount,
                        start = expectedStart,
                        end = expectedEnd,
                        totalFilesSizeInBytes = expectedTotalFilesSizeInBytes
                )

                Then("batch stats object is correctly populated") {

                    actualSecuredFilesBatchStats.filesCount shouldBe expectedFilesCount
                    actualSecuredFilesBatchStats.start shouldBe expectedStart
                    actualSecuredFilesBatchStats.end shouldBe expectedEnd
                    actualSecuredFilesBatchStats.totalFilesSizeInBytes shouldBe expectedTotalFilesSizeInBytes
                    actualSecuredFilesBatchStats.duration shouldBe expectedDuration
                    actualSecuredFilesBatchStats.securedBytesPerSec shouldBe expectedSecuredBytesPerSec
                }
            }
        }
    }
}