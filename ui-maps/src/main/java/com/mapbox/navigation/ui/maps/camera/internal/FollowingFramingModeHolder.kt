package com.mapbox.navigation.ui.maps.camera.internal

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
enum class FollowingFramingMode { NONE, LOCATION_INDICATOR, MULTIPLE_POINTS }

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
class FollowingFramingModeHolder {

    var mode: FollowingFramingMode = FollowingFramingMode.NONE
}
