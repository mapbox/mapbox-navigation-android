package com.mapbox.navigation.base.internal.performance

import android.annotation.SuppressLint
import android.os.Build
import android.os.Trace
import androidx.annotation.RequiresApi
import kotlin.time.Duration

private const val MAPBOX_TRACE_ID = "mbx"
private const val NAV_SDK_PREFIX = "nav-sdk"

@RequiresApi(Build.VERSION_CODES.Q)
internal class TraceSectionsPerformanceObserver : PerformanceObserver {

    @SuppressLint("UnclosedTrace")
    override fun syncSectionStarted(name: String) {
        Trace.beginSection(wrapSectionName(name))
    }

    override fun syncSectionCompleted(name: String, duration: Duration?) {
        Trace.endSection()
    }

    @SuppressLint("UnclosedTrace")
    override fun asyncSectionStarted(name: String, id: Int) {
        Trace.beginAsyncSection(wrapSectionName(name), id)
    }

    override fun asyncSectionFinished(name: String, id: Int, duration: Duration?) {
        Trace.endAsyncSection(wrapSectionName(name), id)
    }

    private fun wrapSectionName(name: String) = "$MAPBOX_TRACE_ID: $NAV_SDK_PREFIX: $name"
}

internal fun getTraceSectionsPerformanceObserver() =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        TraceSectionsPerformanceObserver()
    } else {
        null
    }
