package com.mapbox.navigation.core.internal

import androidx.annotation.RestrictTo
import com.mapbox.navigation.core.RoutesSetError

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun createRoutesSetError(message: String): RoutesSetError = RoutesSetError(message)
