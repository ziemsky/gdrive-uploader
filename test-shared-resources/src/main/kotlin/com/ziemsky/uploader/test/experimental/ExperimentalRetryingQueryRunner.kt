package com.ziemsky.uploader.test.experimental

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.drive.Drive
import kotlinx.coroutines.*
import mu.KotlinLogging
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.Executors
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.pow


class ExperimentalRetryingQueryRunner(private val drive: Drive) {
    private val log = KotlinLogging.logger {}

    private val executorCoroutineDispatcher = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1).asCoroutineDispatcher()


    fun executeParallelQueries(times: Int) {

        log.debug { "Before launching $times queries" }

        val timeStartAllQueries = Instant.now()

        executorCoroutineDispatcher.use {

            var tasks = runBlocking {
                (1..times).map { async { querySingleSuspending(it, executorCoroutineDispatcher) } }
            }

            val completedQueries = tasks.filter { deferred -> deferred.isCompleted }
            val completedQueriesCount = completedQueries.count()

            val failedCompletedQueriesCount = completedQueries
                    .map { deferred -> deferred.getCompleted() }
                    .filter { queryStatus -> queryStatus.equals(QueryStatus.FAILURE) }
                    .count()

            log.debug { "Completed $completedQueriesCount out of $times queries with $failedCompletedQueriesCount failed; in ${Duration.between(timeStartAllQueries, Instant.now())}" }
        }
    }

    enum class QueryStatus {
        SUCCESS,
        FAILURE
    }

    private suspend fun querySingleSuspending(i: Int, coroutineDispatcher: ExecutorCoroutineDispatcher): QueryStatus = withContext(coroutineDispatcher) {
        val timeStartSingleQuery = Instant.now()

        log.debug { "query [$i] start" }

        var queryStatus: QueryStatus = QueryStatus.FAILURE

        try {
            // result = drive.files().get("root").setFields("id").execute()
            // val result = drive.about().get().setFields("kind").execute()
            queryWithBackoff()

            queryStatus = QueryStatus.SUCCESS

        } catch (e: Exception) {
            log.error(e) { "query [$i] failed" }
        }

        log.debug { "query [$i] completed in ${Duration.between(timeStartSingleQuery, Instant.now())} with $queryStatus" }

        queryStatus
    }

    private fun queryWithBackoff() {

        // @formatter:on
        retryOnException(
                action = { query() },
                isRetryableExceptionPredicate = { throwable ->
                    throwable is GoogleJsonResponseException
                            && throwable.statusCode == 403
                            && with(throwable.details.errors) {
                        isNotEmpty()
                                && this[0].domain == "usageLimits"
                                && this[0].reason == "userRateLimitExceeded"
                    }
                },
                timeOut = Duration.of(10, ChronoUnit.SECONDS),
                actionOnExpiration = { log.error { "expired" } }
        )
        // @formatter:on
    }


    // todo default request rate, to minimise getting exception to begin with
    // todo block parallel requests

    private val retryLock: Lock = ReentrantLock()

    private val initialRetryWaitInMillis = 200

    private fun retryOnExceptionReduceNumberOfRetryingThreads(
            action: () -> Unit,
            isRetryableExceptionPredicate: (Throwable) -> Boolean,
            maxRetryCount: Int,
            timeOut: Duration,
            actionOnExpiration: () -> Unit
    ) {
        var retryLockOwnedByThisThread = false

        var nextAttemptDelay = Duration.ZERO

        var throwable: Throwable? = null

        // lock on common resource and unlock immediately
        // if the resource has been locked by other thread for retrying, it'll stop us from sending request
        // until retry sequence is over
        try {
            log.debug { "Is retry in progress? (attempting to acquire retryLock)" }
            retryLock.lock()
            log.debug { "No retry in progress (retryLock acquired)" }
        } finally {
            log.debug { "No retry in progress (retryLock released)" }
            retryLock.unlock()
        }

        for (attemptNumber in 1..maxRetryCount) {

            log.debug { "attempt $attemptNumber" }

            runBlocking {
                log.debug { "Delaying attempt $attemptNumber by $nextAttemptDelay" }
                delay(nextAttemptDelay.toMillis())
            }

            log.debug { "Action start" }
            val result: Result<Unit> = runCatching(action)
            log.debug { "Action stop" }

            if (result.isSuccess) {

                // if it's us that locked the common resource - unlock the common resource to indicate retry finish
                if (retryLockOwnedByThisThread) {
                    log.debug { "Unlocking owned lock after succesful retry" }
                    retryLock.unlock()
                }

                break // success - we're done

            } else {

                if (isRetryableExceptionPredicate.invoke(result.exceptionOrNull()!!)) {

                    log.error { "Retryable exception caught" }

                    // lock common resource to indicate retry in progress
                    // record the fact that it's us that locked it
                    retryLock.lock()
                    log.debug { "Acquired retry lock" }
                    retryLockOwnedByThisThread = true


                    nextAttemptDelay = Duration.ofMillis(initialRetryWaitInMillis * (2.0.pow(attemptNumber) - 1).toLong())

                    continue // retryable exception was thrown - continue with next attempt
                } else {
                    throwable = result.exceptionOrNull()!!
                    break // non-retryable exception was thrown - stop retrying
                }
            }
        }

        throwable?.let { throw it }
    }

    private fun retryOnException(
            action: () -> Unit,
            isRetryableExceptionPredicate: (Throwable) -> Boolean,
            maxRetryCount: Int,
            timeOut: Duration,
            actionOnExpiration: () -> Unit
    ) {
        var nextAttemptDelay = Duration.of(0, ChronoUnit.SECONDS)

        var throwable: Throwable? = null

        for (attemptNumber in 1..maxRetryCount) {

            log.info { "attempt $attemptNumber" }

            runBlocking {
                log.info("Delaying attempt $attemptNumber by $nextAttemptDelay")
                delay(nextAttemptDelay.toMillis())

                nextAttemptDelay = Duration.ofMillis(initialRetryWaitInMillis * (2.0.pow(attemptNumber) - 1).toLong())
            }

            val result: Result<Unit> = runCatching(action)

            if (result.isSuccess) {
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

        throwable?.let { throw it }
    }

    private fun retryOnException(
            action: () -> Unit,
            isRetryableExceptionPredicate: (Throwable) -> Boolean,
            timeOut: Duration,
            actionOnExpiration: () -> Unit
    ) {
        var nextAttemptDelay = Duration.of(0, ChronoUnit.SECONDS)

        var throwable: Throwable? = null

        var timeLeft = timeOut

        var attemptNumber = 0

        while (!(timeLeft.isNegative || timeLeft.isZero)) {

            val timeStart = Instant.now()

            attemptNumber++

            log.debug { "Attempt $attemptNumber" }

            runBlocking {
                log.debug { "Delaying attempt $attemptNumber by $nextAttemptDelay" }
                delay(nextAttemptDelay.toMillis())

                nextAttemptDelay = Duration.ofMillis(initialRetryWaitInMillis * (2.0.pow(attemptNumber) - 1).toLong())
            }

            val result: Result<Unit> = runCatching(action)


            val currentIterationDuration = Duration.between(timeStart, Instant.now())

            timeLeft = timeLeft.minus(currentIterationDuration)


            if (result.isSuccess) {
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
    }


    private fun query() {
        drive.files().list().setFields("files(id, name, mimeType)").execute()
    }

}