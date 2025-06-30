package com.mapbox.navigation.base.internal.performance

import android.annotation.SuppressLint
import android.os.Build
import android.os.Trace
import androidx.annotation.RequiresApi
import kotlin.time.Duration

private const val MAPBOX_TRACE_ID = "mbx"

@RequiresApi(Build.VERSION_CODES.Q)
internal class TraceSectionsPerformanceObserver : PerformanceObserver {

    @SuppressLint("UnclosedTrace")
    override fun syncSectionStarted(name: String) {
        Trace.beginSection(wrapSectionName(name))
    }

    override fun syncSectionCompleted(name: String, duration: Duration?) {
        Trace.endSection()
    }

    private fun wrapSectionName(name: String) = "$MAPBOX_TRACE_ID: $name"
}

internal fun getTraceSectionsPerformanceObserver() =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        TraceSectionsPerformanceObserver()
    } else {
        null
    }
