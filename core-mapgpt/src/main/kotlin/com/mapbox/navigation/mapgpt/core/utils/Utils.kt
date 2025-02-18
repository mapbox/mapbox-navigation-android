package com.mapbox.navigation.mapgpt.core.utils

fun Throwable.obfuscateStackTrace(): String =
    "access_token=([^\\s\\n&?]+)".toRegex().replace(stackTraceToString()) { matchResult ->
        val token = matchResult.groupValues[1]
        val redactedToken = "****" + token.takeLast(4)
        "access_token=$redactedToken"
    }
