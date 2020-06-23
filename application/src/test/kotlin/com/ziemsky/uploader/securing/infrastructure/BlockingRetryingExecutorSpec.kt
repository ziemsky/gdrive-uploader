package com.ziemsky.uploader.securing.infrastructure

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.delay
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

class BlockingRetryingExecutorSpec : BehaviorSpec() {
    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf

    init {

        Given("Action that does not throw any exception") {

            val actionOnExpiration: () -> Unit = mockk("actionOnExpiration")

            val successfulAction: () -> Unit = mockk("successfulAction")
            every { successfulAction.invoke() } returns Unit

            mockkObject(Blocker)
            val delayDurationCaptureSlot = slot<Duration>()
            coEvery { Blocker.blockFor(capture(delayDurationCaptureSlot)) } coAnswers { Unit }

            When("attempting to execute action") {

                retryActionWith(
                        actionOnExpiration = actionOnExpiration,
                        action = successfulAction
                )

                Then("action succeeds on first attempt and without delay") {

                    verify(exactly = 1) { successfulAction.invoke() }

                    verify(exactly = 1) { Blocker.blockFor(Duration.ZERO) }

                    confirmVerified(successfulAction, actionOnExpiration, Blocker)
                }
            }
        }


        Given("Action that throws retryable exception on first two attempts within timeout") {

            val retryableException: Exception = Exception("retryableException")

            val retryableExceptionPredicate: (Throwable) -> Boolean = { throwable -> throwable == retryableException }

            val actionOnExpiration: () -> Unit = mockk("actionOnExpiration")

            val eventuallySuccessfulAction: () -> Unit = mockk("eventuallySuccessfulAction")

            every { eventuallySuccessfulAction.invoke() }
                    .throws(retryableException)
                    .andThenThrows(retryableException)
                    .andThen { Unit }

            mockkObject(Blocker)
            val delayDurationCaptureSlot = slot<Duration>()
            coEvery { Blocker.blockFor(capture(delayDurationCaptureSlot)) } coAnswers { Unit }

            When("attempting to execute action") {
                retryActionWith(
                        retryableExceptionPredicate = retryableExceptionPredicate,
                        actionOnExpiration = actionOnExpiration,
                        action = eventuallySuccessfulAction
                )

                Then("tries to execute action three times and does not execute expiration action") {

                    verify(exactly = 3) { eventuallySuccessfulAction.invoke() }

                    verify(exactly = 0) { actionOnExpiration.invoke() }

                    confirmVerified(eventuallySuccessfulAction, actionOnExpiration)
                }
            }
        }


        Given("Action that keeps throwing retryable exception until timeout") {

            val retryableException: Exception = Exception("retryableException")

            val retryableExceptionPredicate: (Throwable) -> Boolean = { throwable -> throwable == retryableException }

            val actionOnExpiration: () -> Unit = mockk("actionOnExpiration")
            every { actionOnExpiration.invoke() } returns Unit

            val alwaysFailingAction: () -> Unit = mockk("alwaysFailingAction")
            every { alwaysFailingAction.invoke() } throws retryableException

            mockkObject(Blocker)
            val delayDurationCaptureSlot = slot<Duration>()
            coEvery { Blocker.blockFor(capture(delayDurationCaptureSlot)) } coAnswers {
                delay(200)
                Unit
            }


            When("timeout is reached whilst re-trying the action") {
                retryActionWith(
                        retryableExceptionPredicate = retryableExceptionPredicate,
                        actionOnExpiration = actionOnExpiration,
                        action = alwaysFailingAction,
                        timeout = Duration.ofMillis(300)
                )

                Then("executes given 'action on expiration' and does not attempt another retry") {

                    verify(exactly = 2) { alwaysFailingAction.invoke() }

                    verify(exactly = 1) { actionOnExpiration.invoke() }

                    confirmVerified(alwaysFailingAction, actionOnExpiration)
                }
            }
        }


        Given("Action that keeps throwing retryable exception") {

            val retryableException: Exception = Exception("retryableException")

            val retryableExceptionPredicate: (Throwable) -> Boolean = { throwable -> throwable == retryableException }

            val actionOnExpiration: () -> Unit = mockk("actionOnExpiration")
            every { actionOnExpiration.invoke() } returns Unit

            val failureCount = AtomicInteger(0)
            val actionFailingHundredTimes: () -> Unit = {
                if (failureCount.getAndIncrement() <= 20) throw retryableException
            }

            val actualIntervals: MutableList<Duration> = mutableListOf()
            mockkObject(Blocker)
            val delayDurationCaptureSlot = slot<Duration>()
            coEvery { Blocker.blockFor(capture(delayDurationCaptureSlot)) } coAnswers {
                actualIntervals.add(delayDurationCaptureSlot.captured)
                Unit
            }


            When("continuing to retry the action") {

                retryActionWith(
                        retryableExceptionPredicate = retryableExceptionPredicate,
                        actionOnExpiration = actionOnExpiration,
                        action = actionFailingHundredTimes
                )


                Then("intervals between retries follow exponential backoff pattern with 200ms initial interval") {

                    actualIntervals shouldContainExactly listOf(
                            "PT0S",
                            "PT0.2S",
                            "PT0.6S",
                            "PT1.4S",
                            "PT3S",
                            "PT6.2S",
                            "PT12.6S",
                            "PT25.4S",
                            "PT51S",
                            "PT1M42.2S",
                            "PT3M24.6S",
                            "PT6M49.4S",
                            "PT13M39S",
                            "PT27M18.2S",
                            "PT54M36.6S",
                            "PT1H49M13.4S",
                            "PT3H38M27S",
                            "PT7H16M54.2S",
                            "PT14H33M48.6S",
                            "PT29H7M37.4S",
                            "PT58H15M15S",
                            "PT116H30M30.2S"
                    ).map { duration -> Duration.parse(duration) }

                    confirmVerified(actionOnExpiration)
                }
            }
        }

        Given("Action that throws non-retryable exception") {

            val nonRetryableException: Exception = Exception("nonRetryableException")

            val retryableExceptionPredicate: (Throwable) -> Boolean = { throwable -> throwable != nonRetryableException }

            val failingAction: () -> Unit = mockk("eventuallySuccessfulAction")
            every { failingAction.invoke() } throws nonRetryableException

            val actionOnExpiration: () -> Unit = mockk("actionOnExpiration")
            every { actionOnExpiration.invoke() } returns Unit

            mockkObject(Blocker)
            val delayDurationCaptureSlot = slot<Duration>()
            coEvery { Blocker.blockFor(capture(delayDurationCaptureSlot)) } coAnswers { Unit }

            When("attempting to execute action") {

                val actualException = shouldThrow<NonRetryableException> {
                    retryActionWith(
                            retryableExceptionPredicate = retryableExceptionPredicate,
                            actionOnExpiration = actionOnExpiration,
                            action = failingAction
                    )
                }

                Then("re-throws the action and does not attempt another retry") {

                    actualException.cause shouldBe nonRetryableException

                    verify(exactly = 1) { failingAction.invoke() }

                    verify(exactly = 0) { actionOnExpiration.invoke() }

                    confirmVerified(failingAction, actionOnExpiration)
                }
            }
        }
    }

    private fun retryActionWith(
            retryableExceptionPredicate: (Throwable) -> Boolean = { true },
            timeout: Duration = Duration.ofSeconds(5),
            actionOnExpiration: () -> Unit = { /* no-op: not needed here */ },
            action: () -> Unit = { /* no-op: always succeeds */ }
    ) = BlockingRetryingExecutor.retryOnException(
            retryableExceptionPredicate = retryableExceptionPredicate,
            timeout = timeout,
            actionOnExpiration = actionOnExpiration,
            action = action
    )
}