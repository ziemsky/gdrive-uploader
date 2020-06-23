package com.ziemsky.uploader.securing.infrastructure

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.property.arbitrary.arb
import io.kotest.property.exhaustive.exhaustive
import io.kotest.property.forAll
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit.MILLIS

class BlockerSpec : BehaviorSpec() {
    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf

    private val executionDurationTolerance = Duration.of(100, MILLIS)

    init {

        Given("positive duration to block for") {

            val validIntervals = arb { rs ->
                generateSequence {
                    Duration.ofMillis( rs.random.nextLong(2, 2000) )
                }
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


            val nonPositiveIntervals = listOf(-2000L, -1000L, -100L, 0L).map { i -> Duration.ofMillis(i) }.exhaustive()

            When("asked to block") {

                val action = { duration:Duration -> Blocker.blockFor(duration) }

                Then("executes action immediately") {
                    forAll(nonPositiveIntervals) { givenDuration: Duration ->

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
