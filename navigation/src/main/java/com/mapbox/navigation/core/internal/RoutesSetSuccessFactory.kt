package com.mapbox.navigation.core.internal

import androidx.annotation.RestrictTo
import com.mapbox.navigation.core.RoutesSetError
import com.mapbox.navigation.core.RoutesSetSuccess

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun createRoutesSetSuccess(
    ignoredAlternatives: Map<String, RoutesSetError> = emptyMap(),
): RoutesSetSuccess = RoutesSetSuccess(ignoredAlternatives)
