package com.mapbox.navigation.base.route

val List<RouterFailure>?.isRetryable: Boolean
    get() = this?.any { it.isRetryable } ?: false
