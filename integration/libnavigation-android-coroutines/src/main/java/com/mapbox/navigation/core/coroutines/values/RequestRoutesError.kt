package com.mapbox.navigation.core.coroutines.values

import com.mapbox.navigation.base.route.RouterFailure

class RequestRoutesError(
    val reasons: List<RouterFailure>,
    message: String
) : Error(message)
