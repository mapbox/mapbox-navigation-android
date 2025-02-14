package com.mapbox.navigation.core.internal.ev

import androidx.annotation.RestrictTo
import com.mapbox.navigation.core.MapboxNavigation
import kotlinx.coroutines.flow.StateFlow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun MapboxNavigation.internalEvUpdatedData(): StateFlow<Map<String, String>> =
    internalEvUpdatedData()
