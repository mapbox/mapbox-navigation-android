package com.mapbox.navigation.core.telemetry

import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.internal.telemetry.TelemetryAndroidAutoInterface
import com.mapbox.navigation.core.internal.telemetry.UserFeedbackCallback
import com.mapbox.navigation.core.internal.telemetry.UserFeedbackSubscriber
import com.mapbox.navigation.core.internal.telemetry.event.AndroidAutoEvent
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent
import com.mapbox.navigation.core.telemetry.events.FeedbackMetadata
import com.mapbox.navigation.core.telemetry.events.FeedbackMetadataWrapper
import com.mapbox.navigation.core.trip.session.MapboxTripSession
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigator.Telemetry
import com.mapbox.navigator.UserFeedback
import java.util.concurrent.CopyOnWriteArraySet

internal class NavigationTelemetry private constructor(
    private val tripSession: TripSession,
    private val nativeTelemetry: () -> Telemetry,
) : TelemetryAndroidAutoInterface, UserFeedbackSubscriber {

    private val userFeedbackCallbacks = CopyOnWriteArraySet<UserFeedbackCallback>()

    internal companion object {
        operator fun invoke(
            tripSession: TripSession,
            nativeTelemetry: () -> Telemetry
        ): NavigationTelemetry =
            NavigationTelemetry(tripSession, nativeTelemetry)
    }

    @ExperimentalPreviewMapboxNavigationAPI
    fun provideFeedbackMetadataWrapper(): FeedbackMetadataWrapper  =
        when (tripSession.getState()) {
            TripSessionState.STARTED ->
                FeedbackMetadataWrapper(nativeTelemetry().startBuildUserFeedbackMetadata())
            TripSessionState.STOPPED -> throw IllegalStateException(
                "Feedback Metadata might be provided when trip session is started only"
            )
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
        fun notifyUserFeedbackCallbacks(location: Point) {
            val userFeedback = com.mapbox.navigation.core.internal.telemetry.UserFeedback(
                "-1",
                feedbackType,
                feedbackSource,
                description,
                screenshot,
                feedbackSubType,
                location,
            )
            userFeedbackCallback?.onNewUserFeedback(userFeedback)
            for (callback in userFeedbackCallbacks) {
                callback.onNewUserFeedback(userFeedback)
            }
        }

        nativeTelemetry().postUserFeedback(
            feedbackMetadata?.userFeedbackMetadata
                ?: nativeTelemetry().startBuildUserFeedbackMetadata().metadata,
            UserFeedback(
                feedbackType,
                feedbackSubType?.toList() ?: emptyList(),
                feedbackSource,
                description,
                screenshot,
            )
        ) { result ->
            result.fold({

            }, {
                notifyUserFeedbackCallbacks(it)
            })
        }
    }

    fun postCustomEvent(type: String, version: String, payload: String?) {
        nativeTelemetry().postTelemetryCustomEvent(type, version, payload)
    }

    override fun postAndroidAutoEvent(event: AndroidAutoEvent) {
        nativeTelemetry().postOuterDeviceEvent(event.mapToNative())
    }

    override fun registerUserFeedbackCallback(userFeedbackCallback: UserFeedbackCallback) {
        userFeedbackCallbacks.add(userFeedbackCallback)
    }

    override fun unregisterUserFeedbackCallback(userFeedbackCallback: UserFeedbackCallback) {
        userFeedbackCallbacks.remove(userFeedbackCallback)
    }
}
