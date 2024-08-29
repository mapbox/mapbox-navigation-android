package com.mapbox.navigation.core.telemetry

import androidx.annotation.UiThread
import com.mapbox.common.SdkInformation
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
    private lateinit var sdkInformation: SdkInformation

    private var isWrapperInitialized = false
    private var sdkTelemetry: SdkTelemetry = SdkTelemetry.EMPTY

    fun initialize(
        mapboxNavigation: MapboxNavigation,
        navigationOptions: NavigationOptions,
        sdkInformation: SdkInformation,
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
        this.sdkInformation = sdkInformation

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

        sdkTelemetry.destroy(mapboxNavigation)
        sdkTelemetry = SdkTelemetry.EMPTY
    }

    fun postCustomEvent(
        payload: String,
        @CustomEvent.Type customEventType: String,
        customEventVersion: String,
    ) {
        sdkTelemetry.postCustomEvent(
            payload = payload,
            customEventType = customEventType,
            customEventVersion = customEventVersion,
        )
    }

    @ExperimentalPreviewMapboxNavigationAPI
    fun provideFeedbackMetadataWrapper(): FeedbackMetadataWrapper? {
        return sdkTelemetry.provideFeedbackMetadataWrapper()
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
        sdkTelemetry.postUserFeedback(
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
        logD(
            "MapboxMetricsReporter.init from MapboxNavigation main",
            MapboxNavigationTelemetry.LOG_CATEGORY,
        )
        MapboxMetricsReporter.init(sdkInformation)
        MapboxNavigationTelemetry.initialize(
            mapboxNavigation,
            navigationOptions,
            MapboxMetricsReporter,
        )

        sdkTelemetry = MapboxNavigationTelemetry
    }
}
