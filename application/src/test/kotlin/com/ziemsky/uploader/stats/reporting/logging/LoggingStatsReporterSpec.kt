package com.ziemsky.uploader.stats.reporting.logging


import com.ziemsky.uploader.UploaderAbstractBehaviourSpec
import com.ziemsky.uploader.securing.model.SecuredFileSummary
import com.ziemsky.uploader.securing.model.SecuredFilesBatchStats
import com.ziemsky.uploader.stats.StatsCalculator
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class LoggingStatsReporterSpec : UploaderAbstractBehaviourSpec() {

    init {
        val statsLogger: StatsLogger = mockk(relaxed = true)
        val statsCalculator: StatsCalculator = mockk(relaxed = true)

        val loggingStatsReporter: LoggingStatsReporter

        loggingStatsReporter = LoggingStatsReporter(statsCalculator, statsLogger)


        Given("batch of secured files summaries") {

            val securedFileSummaries: Set<SecuredFileSummary> = mockk()
            every { securedFileSummaries.isEmpty() } returns false

            val securedFilesBatchStats: SecuredFilesBatchStats = mockk()

            every { statsCalculator.calculateStatsFor(securedFileSummaries) } returns securedFilesBatchStats


            When("reporting upload stats") {

                loggingStatsReporter.reportStatsForSecuredFiles(securedFileSummaries)


                Then("upload stats are logged") {

                    verify(exactly = 1) { statsLogger.log(securedFilesBatchStats) }
                }
            }
        }


        Given("empty batch of secured files summaries") {

            val securedFileSummaries: Set<SecuredFileSummary> = mockk()
            every { securedFileSummaries.isEmpty() } returns true


            When("reporting upload stats") {

                val actualException = shouldThrow<IllegalArgumentException> {
                    loggingStatsReporter.reportStatsForSecuredFiles(securedFileSummaries)
                }


                Then("exception is thrown and upload stats are not calculated or logged") {

                    actualException.message shouldBe "Secured files summaries are required but none were provided."

                    verify {
                        statsCalculator wasNot Called
                        statsLogger wasNot Called
                    }
                }
            }
        }
    }
}
