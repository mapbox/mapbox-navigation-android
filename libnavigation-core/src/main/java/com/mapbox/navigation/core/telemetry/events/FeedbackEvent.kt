package com.mapbox.navigation.core.telemetry.events

import android.annotation.SuppressLint
import androidx.annotation.StringDef

/**
 * Documentation is here [https://paper.dropbox.com/doc/Navigation-Telemetry-Events-V1--AuUz~~~rEVK7iNB3dQ4_tF97Ag-iid3ZImnt4dsW7Z6zC3Lc]
 */

@SuppressLint("ParcelCreator")
class FeedbackEvent {
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
