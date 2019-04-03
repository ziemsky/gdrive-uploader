package com.ziemsky.uploader


import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.ziemsky.uploader.Lines.Line
import com.ziemsky.uploader.Lines.Lines
import io.kotlintest.IsolationMode
import io.kotlintest.inspectors.forAll
import io.kotlintest.shouldBe
import io.kotlintest.specs.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import org.slf4j.LoggerFactory
import java.util.stream.Stream

class Slf4jStatsLoggerSpec: BehaviorSpec() {
    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf

    init {
        val statsLogRenderer: StatsLogRenderer = mockk()

        val statsLogger: StatsLogger = Slf4jStatsLogger(statsLogRenderer)

        val appender = ListAppender<ILoggingEvent>()
        appender.start()

        (LoggerFactory.getLogger(Slf4jStatsLogger::class.java) as Logger).addAppender(appender)


        Given("secured files batch stats") {

            val securedFilesBatchStats: SecuredFilesBatchStats = mockk()

            val lines: Lines = mockk {
                every { stream() } returns Stream.of(
                        Line("log line one"),
                        Line("log line two"),
                        Line("log line three")
                )
            }

            every { statsLogRenderer.render(securedFilesBatchStats) } returns lines


            When("logging upload stats") {

                statsLogger.log(securedFilesBatchStats)


                Then("upload stats rendered in appropriate format are logged through the Slf4j logger") {

                    val actualLoggedMessages = appender.list

                    actualLoggedMessages.forAll { it.level shouldBe Level.INFO }

                    actualLoggedMessages.size shouldBe 3
                    actualLoggedMessages[0].formattedMessage shouldBe "log line one"
                    actualLoggedMessages[1].formattedMessage shouldBe "log line two"
                    actualLoggedMessages[2].formattedMessage shouldBe "log line three"

                }
            }
        }
    }
}
