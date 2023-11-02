package com.mapbox.navigation.base.route

val List<RouterFailure>.isRetryable: Boolean
    get() = any { it.isRetryable }
