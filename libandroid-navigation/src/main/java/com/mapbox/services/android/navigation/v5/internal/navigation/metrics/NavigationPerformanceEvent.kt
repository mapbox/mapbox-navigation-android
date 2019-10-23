package com.mapbox.services.android.navigation.v5.internal.navigation.metrics

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import com.mapbox.android.telemetry.Event
import com.mapbox.services.android.navigation.v5.internal.navigation.Attribute
import com.mapbox.services.android.navigation.v5.internal.navigation.DoubleCounter
import com.mapbox.services.android.navigation.v5.internal.navigation.NavigationPerformanceMetadata
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.Locale

@SuppressLint("ParcelCreator")
internal open class NavigationPerformanceEvent(
    private val sessionId: String,
    eventName: String,
    open var metadata: NavigationPerformanceMetadata
) : Event(), Parcelable {

    companion object {
        private const val DATE_AND_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
        private const val PERFORMANCE_TRACE = "mobile.performance_trace"
        private const val EVENT_NAME = "event_name"
        private val dateFormat = SimpleDateFormat(DATE_AND_TIME_PATTERN, Locale.US)
    }

    private val event: String
    private val created: String
    private val counters: MutableList<DoubleCounter>
    private val attributes: MutableList<Attribute>

    init {
        event = PERFORMANCE_TRACE
        created = obtainCurrentDate()
        counters = ArrayList()
        attributes = ArrayList()
        attributes.add(Attribute(EVENT_NAME, eventName))
    }

    private fun obtainCurrentDate(): String {
        return dateFormat.format(Date())
    }

    fun addCounter(counter: DoubleCounter) {
        counters.add(counter)
    }

    fun addAttribute(attribute: Attribute) {
        attributes.add(attribute)
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) {}
}
