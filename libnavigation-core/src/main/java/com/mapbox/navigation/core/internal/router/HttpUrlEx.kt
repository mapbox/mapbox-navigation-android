package com.mapbox.navigation.core.internal.router

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

internal const val ACCESS_TOKEN_QUERY_PARAM = "access_token"
internal const val REDACTED = "redacted"

internal fun String.redactQueryParam(queryParameterName: String): String {
    val httpUrl: HttpUrl = this.toHttpUrl()
    return httpUrl.redactQueryParam(queryParameterName).toString()
}

internal fun HttpUrl.redactQueryParam(queryParameterName: String): HttpUrl {
    return if (this.queryParameter(queryParameterName) != null) {
        this.newBuilder()
            .removeAllQueryParameters(queryParameterName)
            .addQueryParameter(
                queryParameterName,
                REDACTED,
            )
            .build()
    } else {
        this
    }
}
