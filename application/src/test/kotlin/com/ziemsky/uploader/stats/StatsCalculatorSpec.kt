package com.ziemsky.uploader.stats


import com.ziemsky.uploader.UploaderAbstractBehaviourSpec
import com.ziemsky.uploader.securing.model.SecuredFileSummary
import com.ziemsky.uploader.securing.model.SecuredFilesBatchStats
import com.ziemsky.uploader.securing.model.local.LocalFile
import com.ziemsky.uploader.securing.model.local.LocalFileName
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

class StatsCalculatorSpec() : UploaderAbstractBehaviourSpec() {

    init {
        val statsCalculator = StatsCalculator()


        Given("batch of secured files summaries") {

            val securedFileSummaries: Set<SecuredFileSummary> = setOf(
                    securedFileSummary("2019-08-17T10:15:30.00Z", "2019-08-17T10:15:31.00Z", "20190817_01", 10_000),
                    securedFileSummary("2019-08-17T10:15:30.20Z", "2019-08-17T10:15:31.40Z", "20190817_02", 20_000),
                    securedFileSummary("2019-08-17T10:15:30.30Z", "2019-08-17T10:16:30.50Z", "20190817_03", 30_000)
            )


            When("calculating stats for the batch file") {

            val actualStats: SecuredFilesBatchStats = statsCalculator.calculateStatsFor(securedFileSummaries)

                Then("""stats are calculated:
                    | number of files secured,
                    | total time it took to secure them,
                    | moment the securing started,
                    | moment the securing completed,
                    | total file size,
                    | average speed""".trimMargin()) {

                    val expectedStart = Instant.parse("2019-08-17T10:15:30.00Z")
                    val expectedEnd = Instant.parse("2019-08-17T10:16:30.50Z")

                    withClue("Files count")               { actualStats.filesCount shouldBe 3 }
                    withClue("Upload start")              { actualStats.start shouldBe expectedStart }
                    withClue("Upload end")                { actualStats.end shouldBe expectedEnd }
                    withClue("Upload duration")           { actualStats.duration shouldBe Duration.of(60_500, ChronoUnit.MILLIS) }
                    withClue("Files total size in bytes") { actualStats.totalFilesSizeInBytes shouldBe 60_000 }
                    withClue("Upload speed")              { actualStats.securedBytesPerSec shouldBe 1000 }
                }
            }
        }

//        Given("batch of secured files summaries containing one file summary") {
//            When("calculating stats for the batch file") {
//                Then("""stats are calculated:
//                    | number of files secured,
//                    | total time it took to secure them,
//                    | moment the securing started,
//                    | moment the securing completed,
//                    | total file size,
//                    | average speed""".trimMargin()) {
//
//                    TODO()
//                }
//            }
//        }
//
//        Given("batch of secured files summaries containing no file summaries") {
//            When("calculating stats for the batch file") {
//                Then("") {
//                    TODO("does what?")
//                }
//            }
//        }
    }

    private fun securedFileSummary(uploadStart: String,
                                   uploadEnd: String,
                                   fileName: String,
                                   fileSizeInBytes: Long
    ): SecuredFileSummary {

        val localFile: LocalFile = mockk()

        every { localFile.nameLocal() } returns LocalFileName(fileName)
        every { localFile.sizeInBytes() } returns fileSizeInBytes

        return SecuredFileSummary(
                Instant.parse(uploadStart),
                Instant.parse(uploadEnd),
                localFile
        )
    }

}
