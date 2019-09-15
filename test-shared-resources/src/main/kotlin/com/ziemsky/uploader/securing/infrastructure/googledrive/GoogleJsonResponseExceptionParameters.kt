package com.ziemsky.uploader.securing.infrastructure.googledrive

import com.google.api.client.googleapis.json.GoogleJsonError
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.HttpHeaders
import com.google.api.client.http.HttpResponseException

data class GoogleJsonResponseExceptionParameters(
        val errorInfoDomain: String,
        val errorInfoReason: String,
        val httpResponseCode: Int
) {
    companion object {
        @JvmStatic
        val RETRYABLE = GoogleJsonResponseExceptionParameters(
                errorInfoDomain = "usageLimits",
                errorInfoReason = "userRateLimitExceeded",
                httpResponseCode = 403
        )
    }

    fun asException(): GoogleJsonResponseException {
        val errorInfo = GoogleJsonError.ErrorInfo()
        errorInfo.domain = errorInfoDomain
        errorInfo.reason = errorInfoReason
        //
        val googleJsonError = GoogleJsonError()
        googleJsonError.errors = listOf(errorInfo)
        //
        return GoogleJsonResponseException(
                HttpResponseException.Builder(httpResponseCode, "", HttpHeaders()),
                googleJsonError
        )
    }
}