package com.ziemsky.uploader


import com.ziemsky.uploader.Lines.Line
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

                    actualRenderedLinesText shouldBe """
                        secured files count: 3
                            upload duration: 00:01:01.000
                                upload size: 58.6 KiB
                               upload speed: 983 B/s
                               upload start: 2019-08-18T19:10:00Z
                                 upload end: 2019-08-18T19:11:01Z
                                 """.trimIndent()
                }
            }
        }
    }
}
