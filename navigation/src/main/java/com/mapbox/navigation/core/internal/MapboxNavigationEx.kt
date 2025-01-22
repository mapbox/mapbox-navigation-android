package com.mapbox.navigation.core.internal

import androidx.annotation.RestrictTo
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator

@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
val MapboxNavigation.nativeNavigator: MapboxNativeNavigator
    get() = navigator
