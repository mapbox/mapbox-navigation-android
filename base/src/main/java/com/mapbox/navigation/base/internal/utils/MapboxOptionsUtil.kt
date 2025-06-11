package com.mapbox.navigation.base.internal.utils

import androidx.annotation.RestrictTo
import com.mapbox.common.MapboxOptionsImpl

// TODO Common SDK classes can't be mocked without wrapper because of UnsatisfiedLinkError.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
object MapboxOptionsUtil {

    @JvmStatic
    fun setStagingAccessToken(token: String) {
        MapboxOptionsImpl.setStagingAccessToken(token)
    }

    @JvmStatic
    fun setUseStaging(service: String, useStaging: Boolean) {
        MapboxOptionsImpl.setUseStaging(service, useStaging)
    }

    @JvmStatic
    fun getTokenForService(service: String): String {
        return MapboxOptionsImpl.getTokenForService(service)
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
object NavSDKServices {
    const val ISOCHRONE = "ISOCHRONE"
    const val CHM = "CHM"
}
