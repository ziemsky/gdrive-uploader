package com.ziemsky.uploader.test.shared

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.time.Duration
import java.time.Instant
import kotlin.math.pow

class RetryingExecutor {

    companion object {
        private val log = KotlinLogging.logger {}

        private val initialRetryWaitInMillis = 200

        fun <ACTION_RESULT> retryOnException(
                action: () -> ACTION_RESULT,
                isRetryableExceptionPredicate: (Throwable) -> Boolean,
                timeOut: Duration,
                actionOnExpiration: () -> Unit
        ): ACTION_RESULT {

            var nextAttemptDelay = Duration.ZERO

            var throwable: Throwable? = null

            var timeLeft = timeOut

            var attemptNumber = 0

            var actionResult: ACTION_RESULT? = null

            while (!(timeLeft.isNegative || timeLeft.isZero)) {

                val timeStart = Instant.now()

                attemptNumber++

                if (attemptNumber > 1) log.debug { "Attempt $attemptNumber" }

                runBlocking {
                    if (!nextAttemptDelay.isZero) log.debug { "Delaying attempt $attemptNumber by $nextAttemptDelay" }
                    delay(nextAttemptDelay.toMillis())

                    nextAttemptDelay = Duration.ofMillis(initialRetryWaitInMillis * (2.0.pow(attemptNumber) - 1).toLong())
                }

                val result: Result<ACTION_RESULT> = runCatching(action)


                val currentIterationDuration = Duration.between(timeStart, Instant.now())

                timeLeft = timeLeft.minus(currentIterationDuration)


                if (result.isSuccess) {

                    actionResult = result.getOrThrow()

                    break // success - we're done

                } else {

                    if (isRetryableExceptionPredicate.invoke(result.exceptionOrNull()!!)) {
                        log.error { "Retryable exception caught" }
                        continue // retryable exception was thrown - continue with next attempt
                    } else {
                        throwable = result.exceptionOrNull()!!
                        break // non-retryable exception was thrown - stop retrying
                    }
                }
            }

            if (timeLeft.isNegative || timeLeft.isZero) {
                actionOnExpiration.invoke()
            }

            throwable?.let { throw it }

            if (actionResult == null) throw IllegalStateException()

            return actionResult
        }

    }

}
