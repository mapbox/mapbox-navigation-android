package com.mapbox.navigation.base.internal.route

import com.mapbox.navigation.base.route.RouterFailure

/**
 * It exists in order not to break API of data class [RouterFailure] by adding boolean field,
 * instead existing fields are used to transfer more data.
 */
@Deprecated("replace by a boolean filed in RouterFailure")
class RetryableThrowable : Throwable(message = "It makes sense to retry in case of that error")