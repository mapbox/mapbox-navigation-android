package com.mapbox.navigation.base.route

/**
 * Indicates if it makes sense to retry for this type of failures.
 * If false, it doesn't make sense to retry route request
 */
val List<RouterFailure>?.isRetryable: Boolean
    get() = this?.any { it.isRetryable } ?: false
