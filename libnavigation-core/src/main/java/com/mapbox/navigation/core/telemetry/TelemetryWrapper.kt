package com.mapbox.navigation.core.telemetry

import androidx.annotation.UiThread
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.telemetry.CustomEvent
import com.mapbox.navigation.core.internal.telemetry.UserFeedbackCallback
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent
import com.mapbox.navigation.core.telemetry.events.FeedbackMetadata
import com.mapbox.navigation.core.telemetry.events.FeedbackMetadataWrapper
import com.mapbox.navigation.metrics.MapboxMetricsReporter
import com.mapbox.navigation.metrics.internal.TelemetryUtilsDelegate
import com.mapbox.navigation.utils.internal.assertDebug
import com.mapbox.navigation.utils.internal.logD

/**
 * Class that manages [MapboxNavigationTelemetry] initialisation. Listens to telemetry state events
 * and can enable/disable telemetry in runtime.
 *
 * TODO(NAVAND-1820) ensure that we have 1-1 relationship between [TelemetryWrapper] and [MapboxNavigationTelemetry]
 * [MapboxNavigationTelemetry] is very complex already and needs to be refactored.
 */
@UiThread
internal class TelemetryWrapper {

    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var navigationOptions: NavigationOptions
    private lateinit var userAgent: String

    private var isWrapperInitialized = false
    private var isTelemetryEnabled = false

    fun initialize(
        mapboxNavigation: MapboxNavigation,
        navigationOptions: NavigationOptions,
        userAgent: String
    ) {
        assertDebug(!isWrapperInitialized) {
            "Already initialized"
        }

        if (isWrapperInitialized) {
            return
        }

        isWrapperInitialized = true

        this.mapboxNavigation = mapboxNavigation
        this.navigationOptions = navigationOptions
        this.userAgent = userAgent

        if (TelemetryUtilsDelegate.getEventsCollectionState()) {
            initializeSdkTelemetry()
        }
    }

    fun destroy() {
        assertDebug(isWrapperInitialized) {
            "Initialize object first"
        }

        if (!isWrapperInitialized) {
            return
        }

        isWrapperInitialized = false

        if (isTelemetryEnabled) {
            unInitializeSdkTelemetry()
        }
    }

    fun postCustomEvent(
        payload: String,
        @CustomEvent.Type customEventType: String,
        customEventVersion: String,
    ) {
        if (!isTelemetryEnabled) {
            return
        }

        MapboxNavigationTelemetry.postCustomEvent(
            payload = payload,
            customEventType = customEventType,
            customEventVersion = customEventVersion
        )
    }

    @ExperimentalPreviewMapboxNavigationAPI
    fun provideFeedbackMetadataWrapper(): FeedbackMetadataWrapper? {
        return if (isTelemetryEnabled) {
            MapboxNavigationTelemetry.provideFeedbackMetadataWrapper()
        } else {
            null
        }
    }

    @ExperimentalPreviewMapboxNavigationAPI
    fun postUserFeedback(
        feedbackType: String,
        description: String,
        @FeedbackEvent.Source feedbackSource: String,
        screenshot: String,
        feedbackSubType: Array<String>?,
        feedbackMetadata: FeedbackMetadata?,
        userFeedbackCallback: UserFeedbackCallback?,
    ) {
        if (!isTelemetryEnabled) {
            return
        }

        MapboxNavigationTelemetry.postUserFeedback(
            feedbackType,
            description,
            feedbackSource,
            screenshot,
            feedbackSubType,
            feedbackMetadata,
            userFeedbackCallback,
        )
    }

    private fun initializeSdkTelemetry() {
        val token = navigationOptions.accessToken ?: return

        logD(
            "MapboxMetricsReporter.init from MapboxNavigation main",
            MapboxNavigationTelemetry.LOG_CATEGORY
        )
        MapboxMetricsReporter.init(
            navigationOptions.applicationContext,
            token,
            userAgent
        )
        MapboxNavigationTelemetry.initialize(
            mapboxNavigation,
            navigationOptions,
            MapboxMetricsReporter,
        )

        isTelemetryEnabled = true
    }

    private fun unInitializeSdkTelemetry() {
        MapboxNavigationTelemetry.destroy(mapboxNavigation)
        isTelemetryEnabled = false
    }
}
