package com.mapbox.navigation.base.internal

import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterFailureType
import com.mapbox.navigation.base.route.RouterOrigin
import java.net.URL

object RouterFailureFactory {

    fun create(
        url: URL,
        @RouterOrigin routerOrigin: String,
        message: String,
        @RouterFailureType type: String,
        throwable: Throwable? = null,
        isRetryable: Boolean = false,
    ) = RouterFailure(
        url = url,
        routerOrigin = routerOrigin,
        message = message,
        type = type,
        throwable = throwable,
        isRetryable = isRetryable,
    )
}
