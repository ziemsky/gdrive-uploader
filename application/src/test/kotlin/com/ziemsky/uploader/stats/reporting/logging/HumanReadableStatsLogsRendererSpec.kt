package com.ziemsky.uploader.stats.reporting.logging


import com.ziemsky.uploader.UploaderAbstractBehaviourSpec
import com.ziemsky.uploader.securing.model.SecuredFilesBatchStats
import com.ziemsky.uploader.stats.reporting.logging.model.Line
import io.kotlintest.shouldBe
import java.time.Instant
import java.util.stream.Collectors.joining

class HumanReadableStatsLogsRendererSpec : UploaderAbstractBehaviourSpec() {

    init {

        val humanReadableStatsLogsRenderer = HumanReadableStatsLogsRenderer()

        Given("secured files batch stats") {

            val securedFilesBatchStats = SecuredFilesBatchStats(
                    3,
                    Instant.parse("2019-08-18T19:10:00Z"),
                    Instant.parse("2019-08-18T19:11:01Z"),
                    60000
            )

            When("rendering stats") {

                val actualRenderedLines = humanReadableStatsLogsRenderer.render(securedFilesBatchStats)

                Then("renders log lines, each with individual stat") {

                    val actualRenderedLinesText = actualRenderedLines.stream()
                            .map(Line::text)
                            .collect(joining("\n"))

                    // @formatter:off
                    actualRenderedLinesText shouldBe """
                        ---------------------------------------
                           Secured files: 3
                         Upload duration: 00:01:01.000
                             Upload size: 58.6 KiB
                            Upload speed: 983 B/s
                            Upload start: 2019-08-18T19:10:00Z
                              Upload end: 2019-08-18T19:11:01Z
                        ---------------------------------------
                        """.trimIndent()
                    // @formatter:on
                }
            }
        }
    }
}
