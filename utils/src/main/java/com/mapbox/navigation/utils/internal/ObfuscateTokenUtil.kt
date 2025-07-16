package com.mapbox.navigation.utils.internal

private val ACCESS_TOKEN_REGEX = "access_token=([^\\s\\n&?]+)".toRegex()

fun String.obfuscateAccessToken() = ACCESS_TOKEN_REGEX.replace(this) { matchResult ->
    val token = matchResult.groupValues[1]
    val redactedToken = "****" + token.takeLast(4)
    "access_token=$redactedToken"
}
