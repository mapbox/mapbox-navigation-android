package com.mapbox.services.android.navigation.v5.internal.navigation

import android.content.Context
import android.location.Location
import com.mapbox.android.telemetry.AppUserTurnstile
import com.mapbox.android.telemetry.MapboxTelemetry
import com.mapbox.services.android.navigation.BuildConfig
import com.mapbox.services.android.navigation.v5.internal.navigation.metrics.NavigationEventFactory
import com.mapbox.services.android.navigation.v5.internal.navigation.metrics.PhoneState
import com.mapbox.services.android.navigation.v5.internal.navigation.metrics.RerouteEvent
import com.mapbox.services.android.navigation.v5.internal.navigation.metrics.SessionState
import com.mapbox.services.android.navigation.v5.internal.navigation.routeprogress.MetricsRouteProgress

internal object NavigationMetricsWrapper {
    private lateinit var sdkIdentifier: String
    // TODO Where are we going to create MapboxTelemetry instance? Which class is going to hold it?
    private lateinit var mapboxTelemetry: MapboxTelemetry

    @JvmStatic
    fun init(
        context: Context,
        accessToken: String,
        userAgent: String,
        sdkIdentifier: String
    ) {
        this.sdkIdentifier = sdkIdentifier
        this.mapboxTelemetry = MapboxTelemetry(context, accessToken, userAgent)
        this.mapboxTelemetry.enable()
    }

    @JvmStatic
    fun toggleLogging(isDebugLoggingEnabled: Boolean) {
        mapboxTelemetry.updateDebugLoggingEnabled(isDebugLoggingEnabled)
    }

    @JvmStatic
    fun disable() {
        mapboxTelemetry.disable()
    }

    @JvmStatic
    fun arriveEvent(
        sessionState: SessionState,
        metricProgress: MetricsRouteProgress,
        location: Location,
        context: Context
    ) {
        val arriveEvent = NavigationEventFactory
            .buildNavigationArriveEvent(
                PhoneState(context),
                sessionState,
                metricProgress,
                location,
                sdkIdentifier
            )
        mapboxTelemetry.push(arriveEvent)
    }

    @JvmStatic
    fun cancelEvent(
        sessionState: SessionState,
        metricProgress: MetricsRouteProgress,
        location: Location,
        context: Context
    ) {
        val cancelEvent = NavigationEventFactory
            .buildNavigationCancelEvent(
                PhoneState(context),
                sessionState,
                metricProgress,
                location,
                sdkIdentifier
            )
        mapboxTelemetry.push(cancelEvent)
    }

    fun departEvent(
        sessionState: SessionState,
        metricsRouteProgress: MetricsRouteProgress,
        location: Location,
        context: Context
    ) {
        val departEvent = NavigationEventFactory
            .buildNavigationDepartEvent(
                PhoneState(context),
                sessionState,
                metricsRouteProgress,
                location,
                sdkIdentifier
            )
        mapboxTelemetry.push(departEvent)
    }

    @JvmStatic
    fun rerouteEvent(
        rerouteEvent: RerouteEvent,
        metricProgress: MetricsRouteProgress,
        location: Location,
        context: Context
    ) {
        val navRerouteEvent = NavigationEventFactory.buildNavigationRerouteEvent(
            PhoneState(context),
            rerouteEvent.sessionState,
            metricProgress,
            location,
            sdkIdentifier,
            rerouteEvent
        )
        mapboxTelemetry.push(navRerouteEvent)
    }

    @JvmStatic
    fun feedbackEvent(
        sessionState: SessionState,
        metricProgress: MetricsRouteProgress,
        location: Location,
        description: String,
        feedbackType: String,
        screenshot: String,
        feedbackSource: String,
        context: Context
    ) {
        val feedbackEvent = NavigationEventFactory.buildNavigationFeedbackEvent(
            PhoneState(context),
            sessionState,
            metricProgress,
            location,
            sdkIdentifier,
            description,
            feedbackType,
            screenshot,
            feedbackSource
        )
        mapboxTelemetry.push(feedbackEvent)
    }

    @JvmStatic
    fun routeRetrievalEvent(
        elapsedTime: Double,
        routeUuid: String,
        sessionId: String,
        metadata: NavigationPerformanceMetadata
    ) {
        mapboxTelemetry.push(RouteRetrievalEvent(elapsedTime, routeUuid, sessionId, metadata))
    }

    @JvmStatic
    fun sendInitialGpsEvent(
        elapsedTime: Double,
        sessionId: String,
        metadata: NavigationPerformanceMetadata
    ) {
        mapboxTelemetry.push(InitialGpsEvent(elapsedTime, sessionId, metadata))
    }

    @JvmStatic
    fun sendTurnstileEvent() {
        mapboxTelemetry.push(AppUserTurnstile(sdkIdentifier, BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME))
    }
}
