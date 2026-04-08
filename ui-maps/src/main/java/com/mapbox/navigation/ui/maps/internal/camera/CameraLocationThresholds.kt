package com.mapbox.navigation.ui.maps.internal.camera

import androidx.annotation.RestrictTo

// Based on the Nav Cpp code:
// https://github.com/mapbox/mapbox-sdk/blob/v0.21.0/projects/navigation-sdk-cpp/modules/base/include/internal/mapbox/navsdk/internal/math.hpp#L112

// Minimum position change (in degrees) required to trigger a location update (~0.1 meter at equator).
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
const val ALMOST_EQUAL_LOCATION_DEGREES = 1e-6

// Minimum bearing change (in degrees) required to trigger a location update.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
const val ALMOST_EQUAL_BEARING_DEGREES = 0.1
