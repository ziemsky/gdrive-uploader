package com.ziemsky.uploader.securing.infrastructure

import io.kotlintest.IsolationMode
import io.kotlintest.properties.Gen
import io.kotlintest.properties.forAll
import io.kotlintest.specs.BehaviorSpec
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit.MILLIS
import kotlin.random.Random

class BlockerSpec : BehaviorSpec() {
    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf

    private val executionDurationTolerance = Duration.of(50, MILLIS)

    init {

        Given("positive duration to block for") {

            val validIntervals = object : Gen<Duration> {

                private val random = Random(Instant.now().toEpochMilli())

                override fun random(): Sequence<Duration> = generateSequence { Duration.ofMillis(random.nextLong(2, 2000)) }

                override fun constants(): Iterable<Duration> = listOf(Duration.ofMillis(1))
            }

            When("asked to block") {

                val action = { duration:Duration -> Blocker.blockFor(duration) }

                Then("blocks for given duration") {
                    forAll(3, validIntervals) { givenDuration: Duration ->

                        val momentStart = Instant.now()

                        action.invoke(givenDuration)

                        val momentEnd = Instant.now()

                        val actualDurationInMillis = Duration.between(momentStart, momentEnd).toMillis()

                        val maxExpectedDuration = givenDuration.plus(executionDurationTolerance)

                        actualDurationInMillis <= maxExpectedDuration.toMillis()
                    }
                }
            }
        }

        Given("non-positive duration to block for") {

            val nonPositiveIntervals = object : Gen<Duration> {

                private val random = Random(Instant.now().toEpochMilli())

                override fun random(): Sequence<Duration> = generateSequence { Duration.ofMillis(-random.nextLong(100, 2000)) }

                override fun constants(): Iterable<Duration> = listOf(Duration.ofMillis(0))
            }

            When("asked to block") {

                val action = { duration:Duration -> Blocker.blockFor(duration) }

                Then("executes action immediately") {
                    forAll(3, nonPositiveIntervals) { givenDuration: Duration ->

                        val momentStart = Instant.now()

                        action.invoke(givenDuration)

                        val momentEnd = Instant.now()

                        val actualDurationInMillis = Duration.between(momentStart, momentEnd).toMillis()

                        actualDurationInMillis <= executionDurationTolerance.toMillis()
                    }
                }
            }
        }
    }
}
