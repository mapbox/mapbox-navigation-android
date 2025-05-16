package com.mapbox.navigation.base.internal.performance

import android.os.Build
import android.os.Trace
import androidx.annotation.RequiresApi
import kotlin.time.Duration

private const val MAPBOX_TRACE_ID = "mbx"

@RequiresApi(Build.VERSION_CODES.Q)
internal class TraceSectionsPerformanceObserver : PerformanceObserver {

    override fun sectionStarted(name: String, id: Int) {
        Trace.beginAsyncSection(wrapSectionName(name), id)
    }

    override fun sectionCompleted(name: String, id: Int, duration: Duration?) {
        Trace.endAsyncSection(wrapSectionName(name), id)
    }

    private fun wrapSectionName(name: String) = "$MAPBOX_TRACE_ID: $name"
}

internal fun getTraceSectionsPerformanceObserver() =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        TraceSectionsPerformanceObserver()
    } else {
        null
    }
