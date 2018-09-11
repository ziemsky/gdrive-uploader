package com.ziemsky.gdriveuploader.test.shared.data

import com.github.tomakehurst.wiremock.client.VerificationException
import com.github.tomakehurst.wiremock.client.WireMock.verify
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import org.awaitility.core.ConditionTimeoutException
import org.awaitility.kotlin.await
import java.util.concurrent.TimeUnit

object wireMockUtils {

    fun verifyEventually(requestPatternBuilder: RequestPatternBuilder) {
        var verificationException: VerificationException? = null

        try {
            await.with()
                    .pollInterval(500, TimeUnit.MILLISECONDS)
                    .timeout(5, TimeUnit.SECONDS)
                    .until {

                        try {
                            verify(requestPatternBuilder)
                            true
                        } catch (e: VerificationException) {
                            verificationException = e
                            false
                        }
                    }
        } catch (e: ConditionTimeoutException) {
            if (verificationException != null) throw verificationException as VerificationException
        }
    }

}