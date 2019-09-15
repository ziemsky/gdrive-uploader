package com.ziemsky.uploader.securing.infrastructure

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.time.Duration
import java.time.Instant

class Blocker {

    companion object {

        private val log = KotlinLogging.logger {}

        fun blockFor(duration: Duration) {

            runBlocking {
                log.trace { "Before delay; now: ${Instant.now()}" }
                delay(duration.toMillis())
                log.trace { "After delay; now: ${Instant.now()}" }
            }
        }
    }
}