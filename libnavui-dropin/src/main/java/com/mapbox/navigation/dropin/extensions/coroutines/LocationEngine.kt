package com.mapbox.navigation.dropin.extensions.coroutines

import android.Manifest
import androidx.annotation.RequiresPermission
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@RequiresPermission(
    anyOf = [
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    ]
)
suspend fun LocationEngine.getLastLocation(): LocationEngineResult =
    suspendCancellableCoroutine { continuation ->
        getLastLocation(object : LocationEngineCallback<LocationEngineResult> {
            override fun onSuccess(result: LocationEngineResult) {
                continuation.resume(result)
            }

            override fun onFailure(exception: Exception) {
                continuation.resumeWithException(exception)
            }
        })
    }
