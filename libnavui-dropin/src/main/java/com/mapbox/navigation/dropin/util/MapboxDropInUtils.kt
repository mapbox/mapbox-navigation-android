package com.mapbox.navigation.dropin.util

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import java.lang.ref.WeakReference

internal object MapboxDropInUtils {

    @SuppressLint("MissingPermission")
    fun getLastLocation(
        context: Context,
        resultConsumer: WeakReference<(Expected<Exception, LocationEngineResult>) -> Unit>
    ) {
        LocationEngineProvider.getBestLocationEngine(context.applicationContext).getLastLocation(
            object : LocationEngineCallback<LocationEngineResult> {
                override fun onSuccess(p0: LocationEngineResult) {
                    resultConsumer.get()?.invoke(ExpectedFactory.createValue(p0))
                }

                override fun onFailure(p0: Exception) {
                    resultConsumer.get()?.invoke(ExpectedFactory.createError(p0))
                }
            }
        )
    }

    fun Boolean.toVisibility() = if (this) {
        View.VISIBLE
    } else {
        View.GONE
    }
}
