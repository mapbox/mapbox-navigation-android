package com.mapbox.navigation.ui.maps.internal.camera

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
enum class FollowingFramingMode {
    LOCATION_INDICATOR,
    MULTIPLE_POINTS,
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
class FollowingFramingModeHolder {

    private val observers = mutableListOf<(FollowingFramingMode?) -> Unit>()

    var prevMode: FollowingFramingMode = FollowingFramingMode.LOCATION_INDICATOR
        private set

    var mode: FollowingFramingMode = FollowingFramingMode.LOCATION_INDICATOR
        set(value) {
            prevMode = field
            field = value
            if (prevMode != value) {
                observers.forEach { it(value) }
            }
        }

    fun addObserver(observer: (FollowingFramingMode?) -> Unit) {
        observers.add(observer)
        observer(mode)
    }

    fun removeObserver(observer: (FollowingFramingMode?) -> Unit) {
        observers.remove(observer)
    }
}
