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

/**
 * Google APIs impose request rate limits.
 *
 * This class is a test bed for experiments with various approaches to dealing with these limits.
 */
class ExperimentalRetryingQueryRunner(private val drive: Drive) {
    private val log = KotlinLogging.logger {}

    private val executorCoroutineDispatcher = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1).asCoroutineDispatcher()

    fun executeQueriesInParallel(requiredQueriesCount: Int) {

        log.debug { "Before launching $requiredQueriesCount queries" }

        val timeStartAllQueries = Instant.now()

        executorCoroutineDispatcher.use {

            var tasks = runBlocking {
                (1..requiredQueriesCount).map { async { querySingleSuspending(it, executorCoroutineDispatcher) } }
            }

            val completedQueries = tasks.filter { deferred -> deferred.isCompleted }
            val completedQueriesCount = completedQueries.count()

            val failedCompletedQueriesCount = completedQueries
                    .map { deferred -> deferred.getCompleted() }
                    .filter { queryStatus -> queryStatus.equals(QueryStatus.FAILURE) }
                    .count()

            val totalDuration = Duration.between(timeStartAllQueries, Instant.now())
            log.debug { "Completed $completedQueriesCount out of $requiredQueriesCount queries with $failedCompletedQueriesCount failed, in $totalDuration. Count of queries completed per sec: ${completedQueriesCount / totalDuration.seconds}" }
        }
    }

    enum class QueryStatus {
        SUCCESS,
        FAILURE
    }

    private suspend fun querySingleSuspending(queryOrdinal: Int, coroutineDispatcher: ExecutorCoroutineDispatcher): QueryStatus = withContext(coroutineDispatcher) {
        val timeStartSingleQuery = Instant.now()

        log.debug { "query [$queryOrdinal] start" }

        var queryStatus: QueryStatus = QueryStatus.FAILURE

        try {
            queryWithBackoff()

            queryStatus = QueryStatus.SUCCESS

        } catch (e: Exception) {
            log.error(e) { "query [$queryOrdinal] failed" }
        }

        log.debug { "query [$queryOrdinal] completed in ${Duration.between(timeStartSingleQuery, Instant.now())} with $queryStatus" }

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


    private val retryLock: Lock = ReentrantLock()

    private val initialRetryWaitInMillis = 200

    /**
     * This method is an abandoned attempt to reduce the number of threads performing retries concurrently.
     *
     * As is, it introduced unacceptable delays and further exploration was deemed not worth the effort.
     *
     * @Deprecated use [retryOnException] instead.
     */
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

    /**
     * Retries given [action] when exception that fulfils [isRetryableExceptionPredicate] is received.
     * Keeps trying until [timeOut] occurs, at which point it invokes [actionOnExpiration].
     * Subsequent retries are performed with increasing delays, following exponential back-off pattern.
     *
     * Takes no account of other threads that may be executing the same [action] in parallel and makes no effort to
     * ensure that only one thread performs the retries.
     *
     * When used to deal with `userRateLimitExceeded` error of Google API, it achieves request rate very closely
     * approaching the quota: tests indicated rate of 9 successful requests per second with quota of 10 requests per
     * second.
     */
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