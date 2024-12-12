package com.mapbox.navigation.core.internal.extensions

import androidx.annotation.RestrictTo
import com.mapbox.common.location.Location
import com.mapbox.navigation.core.navigator.toFixLocation
import com.mapbox.navigator.FixLocation

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun Location.toFixLocation(): FixLocation = this.toFixLocation()
