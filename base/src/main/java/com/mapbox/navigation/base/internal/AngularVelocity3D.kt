package com.mapbox.navigation.base.internal

import androidx.annotation.RestrictTo
import com.mapbox.navigation.base.physics.AngularVelocity3D
import com.mapbox.navigation.base.physics.AngularVelocityUnit
import com.mapbox.navigator.Point3d

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@JvmSynthetic
fun AngularVelocity3D.mapToNativePoint3DRadiansPerSecond(): Point3d {
    return with(convert(AngularVelocityUnit.RADIANS_PER_SECOND)) {
        Point3d(x.toFloat(), y.toFloat(), z.toFloat())
    }
}
