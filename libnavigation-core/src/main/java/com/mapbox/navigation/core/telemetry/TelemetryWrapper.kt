package com.mapbox.navigation.core.telemetry

import androidx.annotation.UiThread
import com.mapbox.common.TelemetryCollectionState
import com.mapbox.common.TelemetryCollectionStateObserver
import com.mapbox.common.TelemetryUtils
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
import java.util.concurrent.CopyOnWriteArrayList

/**
 * This wrapper is needed just for unit tests in order not to deal with types from Common SDK
 */
@UiThread
internal class TelemetryStateWatcher(
    private val telemetryUtils: TelemetryUtils = TelemetryUtils()
) {

    private val observers = CopyOnWriteArrayList<Observer>()

    private val telemetryStateObserver = TelemetryCollectionStateObserver { state ->
        observers.forEach {
            it.onStateChanged(state.isEnabled)
        }
    }

    fun registerObserver(observer: Observer) {
        observers.add(observer)
        if (observers.size == 1) {
            telemetryUtils.registerTelemetryCollectionStateObserver(telemetryStateObserver)
        }
    }

    fun unregisterObserver(observer: Observer) {
        observers.remove(observer)
        if (observers.isEmpty()) {
            telemetryUtils.unregisterTelemetryCollectionStateObserver(telemetryStateObserver)
        }
    }

    fun interface Observer {
        fun onStateChanged(telemetryEnable: Boolean)
    }

    private companion object {
        val TelemetryCollectionState.isEnabled: Boolean
            get() = this == TelemetryCollectionState.ENABLED
    }
}

/**
 * Class that manages [MapboxNavigationTelemetry] initialisation. Listens to telemetry state events
 * and can enable/disable telemetry in runtime.
 *
 * TODO(NAVAND-1820) ensure that we have 1-1 relationship between [TelemetryWrapper] and [MapboxNavigationTelemetry]
 * [MapboxNavigationTelemetry] is very complex already and needs to be refactored.
 */
@UiThread
internal class TelemetryWrapper(
    private val telemetryStateWatcher: TelemetryStateWatcher = TelemetryStateWatcher()
) {

    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var navigationOptions: NavigationOptions
    private lateinit var userAgent: String

    private var isWrapperInitialized = false
    private var isTelemetryEnabled = false

    private val telemetryStateObserver = TelemetryStateWatcher.Observer { isEnabled ->
        if (!isTelemetryEnabled && isEnabled) {
            initializeSdkTelemetry()
        } else if (isTelemetryEnabled && !isEnabled) {
            unInitializeSdkTelemetry()
        }
    }

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

        telemetryStateWatcher.registerObserver(telemetryStateObserver)
    }

    fun destroy() {
        assertDebug(isWrapperInitialized) {
            "Initialize object first"
        }

        if (!isWrapperInitialized) {
            return
        }

        isWrapperInitialized = false

        telemetryStateWatcher.unregisterObserver(telemetryStateObserver)
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
