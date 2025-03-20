package com.mapbox.navigation.ui.maps.internal.camera.lifecycle

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
interface UserLocationIndicatorPositionProvider {
    fun addObserver(observer: UserLocationIndicatorPositionObserver)
    fun removeObserver(observer: UserLocationIndicatorPositionObserver)
}
