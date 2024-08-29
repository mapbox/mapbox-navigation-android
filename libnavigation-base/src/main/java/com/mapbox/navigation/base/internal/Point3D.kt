package com.mapbox.navigation.base.internal

import androidx.annotation.RestrictTo
import com.mapbox.navigation.base.geometry.Point3D

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@JvmSynthetic
fun Point3D.mapToNative(): com.mapbox.navigator.Point3d {
    return com.mapbox.navigator.Point3d(x.toFloat(), y.toFloat(), z.toFloat())
}
