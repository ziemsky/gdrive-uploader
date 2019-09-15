package com.ziemsky.uploader.securing.infrastructure

import com.ziemsky.uploader.securing.infrastructure.Blocker.Companion.blockFor
import mu.KotlinLogging
import java.time.Duration
import java.time.Instant
import kotlin.math.pow

class BlockingRetryingExecutor {

    companion object {
        private val log = KotlinLogging.logger {}

        private const val initialRetryWaitInMillis = 200

        /**
         * Executes given [action] and keeps retrying it as long as it throws an exception that satisfies
         * [retryableExceptionPredicate] (retryable exception) or until [timeout] is reached.
         *
         * If [action] does not throw  any exception, it is considered successful.
         *
         * When the [action] throws an exception that does not satisfy the predicate (non-retryable exception),
         * the exception gets re-thrown to the caller and no more retries are attempted.
         *
         * When [timeout] is reached, method executes [actionOnExpiration].
         *
         * All executions are performed in a blocking manner, on the caller's thread.
         *
         * @param action Action to execute and keep retrying.
         * @param retryableExceptionPredicate Evaluates the exception thrown by the [action] for whether it is
         * 'retryable'; should return `true` if that's the case, `false` otherwise.
         * @param timeout Defines maximum time the retries should be attempted for.
         * @param actionOnExpiration Defines action that should be executed when [timeout] is reached.
         */
        fun retryOnException(
                retryableExceptionPredicate: (Throwable) -> Boolean,
                timeout: Duration,
                actionOnExpiration: () -> Unit,
                action: () -> Unit
        ) {

            var nextAttemptDelay = Duration.ZERO

            var throwable: Throwable? = null

            var timeLeft = timeout

            var attemptNumber = 0

            while (!(timeLeft.isNegative || timeLeft.isZero)) {

                val timeStart = Instant.now()

                attemptNumber++

                log.debug { "Attempt $attemptNumber" }

                log.debug { "Delaying attempt $attemptNumber by $nextAttemptDelay" }

                blockFor(nextAttemptDelay)

                nextAttemptDelay = nextBackOffInterval(attemptNumber, initialRetryWaitInMillis)

                val result: Result<Unit> = runCatching(action)


                val currentIterationDuration = Duration.between(timeStart, Instant.now())

                timeLeft = timeLeft.minus(currentIterationDuration)


                if (result.isSuccess) {

                    if (attemptNumber > 1) {
                        log.warn("There were failed attempts before this one; succeeded on attempt number $attemptNumber")
                    }

                    break // success - we're done

                } else {

                    if (retryableExceptionPredicate.invoke(result.exceptionOrNull()!!)) {
                        log.warn(result.exceptionOrNull()) { "Retryable exception caught" }
                        continue // retryable exception was thrown - continue with next attempt
                    } else {
                        throwable = result.exceptionOrNull()
                        break // non-retryable exception was thrown - stop retrying
                    }
                }
            }

            if (timeLeft.isNegative || timeLeft.isZero) {
                log.debug { "Time out reached. Giving up and invoking provided expiration action." }
                actionOnExpiration.invoke()
            }

            throwable?.let { throw NonRetryableException(it) }
        }

        private fun nextBackOffInterval(attemptNumber: Int, initialIntervalInMillis: Int) =
                Duration.ofMillis(initialIntervalInMillis * (2.0.pow(attemptNumber) - 1).toLong())
    }
}

class NonRetryableException(cause: Throwable) : Exception(cause)