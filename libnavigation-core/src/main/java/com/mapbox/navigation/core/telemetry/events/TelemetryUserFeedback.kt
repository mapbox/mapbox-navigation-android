package com.mapbox.navigation.core.telemetry.events

import android.annotation.SuppressLint
import android.location.Location
import android.os.Parcel
import androidx.annotation.Keep
import androidx.annotation.StringDef
import com.google.gson.Gson
import com.mapbox.android.telemetry.Event
import com.mapbox.navigation.base.metrics.MetricEvent
import com.mapbox.navigation.base.metrics.NavigationMetrics

/**
 * Documentation is here [https://paper.dropbox.com/doc/Navigation-Telemetry-Events-V1--AuUz~~~rEVK7iNB3dQ4_tF97Ag-iid3ZImnt4dsW7Z6zC3Lc]
 */

@Keep
@SuppressLint("ParcelCreator")
class TelemetryUserFeedback(
    @param:FeedbackSource @get:FeedbackSource val feedbackSource: String,
    @param:FeedbackType @get:FeedbackType val feedbackType: String,
    val description: String? = null,
    val userId: String,
    val locationsBefore: Array<Location>,
    val locationsAfter: Array<Location>,
    val feedbackId: String,
    val screenshot: String? = null,
    val step: TelemetryStep? = null,
    var metadata: TelemetryMetadata
) : MetricEvent, Event() {
    val event = NavigationMetrics.FEEDBACK

    override fun writeToParcel(dest: Parcel?, flags: Int) {}

    override fun describeContents() = 0

    override val metricName: String
        get() = NavigationMetrics.FEEDBACK

    override fun toJson(gson: Gson) = gson.toJson(this)

    companion object {
        const val FEEDBACK_TYPE_GENERAL_ISSUE = "general"
        const val FEEDBACK_TYPE_ACCIDENT = "accident"
        const val FEEDBACK_TYPE_HAZARD = "hazard"
        const val FEEDBACK_TYPE_ROAD_CLOSED = "road_closed"
        const val FEEDBACK_TYPE_NOT_ALLOWED = "not_allowed"
        const val FEEDBACK_TYPE_ROUTING_ERROR = "routing_error"
        const val FEEDBACK_TYPE_MISSING_ROAD = "missing_road"
        const val FEEDBACK_TYPE_MISSING_EXIT = "missing_exit"
        const val FEEDBACK_TYPE_CONFUSING_INSTRUCTION = "confusing_instruction"
        const val FEEDBACK_TYPE_INACCURATE_GPS = "inaccurate_gps"
        const val FEEDBACK_SOURCE_REROUTE = "reroute"
        const val FEEDBACK_SOURCE_UI = "user"
    }

    @Retention(AnnotationRetention.SOURCE)
    @StringDef(
        FEEDBACK_TYPE_GENERAL_ISSUE,
        FEEDBACK_TYPE_ACCIDENT,
        FEEDBACK_TYPE_HAZARD,
        FEEDBACK_TYPE_ROAD_CLOSED,
        FEEDBACK_TYPE_NOT_ALLOWED,
        FEEDBACK_TYPE_ROUTING_ERROR,
        FEEDBACK_TYPE_CONFUSING_INSTRUCTION,
        FEEDBACK_TYPE_INACCURATE_GPS,
        FEEDBACK_TYPE_MISSING_ROAD,
        FEEDBACK_TYPE_MISSING_EXIT
    )
    annotation class FeedbackType

    @Retention(AnnotationRetention.SOURCE)
    @StringDef(
        FEEDBACK_SOURCE_REROUTE,
        FEEDBACK_SOURCE_UI
    )
    annotation class FeedbackSource
}
