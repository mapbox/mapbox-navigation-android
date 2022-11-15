package com.mapbox.navigation.instrumentation_tests.utils.idling

import android.location.Location
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.flowLocationMatcherResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

fun MapboxNavigation.firstLocationSync(
    sdkDispatcher: CoroutineDispatcher = Dispatchers.Main
): Location {
    return runBlocking(Dispatchers.Main) {
        this@firstLocationSync.flowLocationMatcherResult().first().enhancedLocation
    }
}
