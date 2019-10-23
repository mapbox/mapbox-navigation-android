package com.mapbox.navigation.metrics

import androidx.annotation.StringDef

interface NavigationMetrics {

    companion object {
        const val ARRIVE = "navigation.arrive"
        const val CANCEL_SESSION = "navigation.cancel"
        const val DEPART = "navigation.depart"
        const val FEEDBACK = "navigation.feedback"
        const val INITIAL_GPS = "initial_gps_event"
        const val BATTERY_EVENT = "battery_event"
        const val APP_USER_TURNSTILE = "appUserTurnstile"
        const val PERFORMANCE = "mobile.performance_trace"
    }

    @StringDef(
        ARRIVE,
        CANCEL_SESSION,
        DEPART,
        FEEDBACK,
        INITIAL_GPS,
        BATTERY_EVENT,
        APP_USER_TURNSTILE,
        PERFORMANCE
    )
    annotation class MetricName

    fun toggleLogging(isDebugLoggingEnabled: Boolean)

    fun arriveEvent(eventName: String, eventJsonString: String)

    fun cancelEvent(eventName: String, eventJsonString: String)

    fun departEvent(eventName: String, eventJsonString: String)

    fun feedbackEvent(eventName: String, eventJsonString: String)

    fun sendInitialGpsEvent(eventName: String, eventJsonString: String)

    fun sendTurnstileEvent(eventName: String, eventJsonString: String)
}