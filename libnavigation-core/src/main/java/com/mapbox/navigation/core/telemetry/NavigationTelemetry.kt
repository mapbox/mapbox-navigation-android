package com.mapbox.navigation.core.telemetry

import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.internal.telemetry.TelemetryAndroidAutoInterface
import com.mapbox.navigation.core.internal.telemetry.UserFeedbackCallback
import com.mapbox.navigation.core.internal.telemetry.UserFeedbackInternal.Companion.toInternal
import com.mapbox.navigation.core.internal.telemetry.UserFeedbackSubscriber
import com.mapbox.navigation.core.internal.telemetry.event.AndroidAutoEvent
import com.mapbox.navigation.core.telemetry.events.FeedbackMetadataWrapper
import com.mapbox.navigation.core.telemetry.events.UserFeedback.Companion.toNative
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigator.Telemetry
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
    fun provideFeedbackMetadataWrapper(): FeedbackMetadataWrapper =
        when (tripSession.getState()) {
            TripSessionState.STARTED ->
                FeedbackMetadataWrapper(nativeTelemetry().startBuildUserFeedbackMetadata())

            TripSessionState.STOPPED -> throw IllegalStateException(
                "Feedback Metadata might be provided when trip session is started only"
            )
        }

    @ExperimentalPreviewMapboxNavigationAPI
    fun postUserFeedback(
        userFeedback: com.mapbox.navigation.core.telemetry.events.UserFeedback,
        userFeedbackCallback: UserFeedbackCallback?,
    ) {
        fun notifyUserFeedbackCallbacks(location: Point) {
            val userFeedbackInternal = userFeedback.toInternal(
                "-1",
                location,
            )
            userFeedbackCallback?.onNewUserFeedback(userFeedbackInternal)
            for (callback in userFeedbackCallbacks) {
                callback.onNewUserFeedback(userFeedbackInternal)
            }
        }

        nativeTelemetry().postUserFeedback(
            userFeedback.feedbackMetadata?.userFeedbackMetadata
                ?: nativeTelemetry().startBuildUserFeedbackMetadata().metadata,
            userFeedback.toNative(),
        ) { result ->
            result.fold({
                // do nothing
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
