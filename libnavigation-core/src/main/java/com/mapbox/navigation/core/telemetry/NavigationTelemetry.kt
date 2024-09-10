package com.mapbox.navigation.core.telemetry

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.internal.telemetry.AndroidAutoEvent
import com.mapbox.navigation.core.internal.telemetry.ExtendedUserFeedback
import com.mapbox.navigation.core.internal.telemetry.UserFeedbackObserver
import com.mapbox.navigation.core.telemetry.UserFeedback.Companion.mapToInternal
import com.mapbox.navigation.core.telemetry.UserFeedback.Companion.mapToNative
import com.mapbox.navigation.core.telemetry.events.FeedbackMetadataWrapper
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigator.Telemetry
import java.util.concurrent.CopyOnWriteArraySet

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class NavigationTelemetry private constructor(
    private val tripSession: TripSession,
    private val nativeNavigator: MapboxNativeNavigator,
) {

    private val userFeedbackObservers = CopyOnWriteArraySet<UserFeedbackObserver>()

    private val telemetry: Telemetry
        get() = nativeNavigator.telemetry

    fun provideFeedbackMetadataWrapper(): FeedbackMetadataWrapper =
        when (tripSession.getState()) {
            TripSessionState.STARTED ->
                FeedbackMetadataWrapper.create(telemetry.startBuildUserFeedbackMetadata())

            TripSessionState.STOPPED -> throw IllegalStateException(
                "Feedback metadata can only be provided when the trip session is started",
            )
        }

    fun postUserFeedback(
        userFeedback: UserFeedback,
        userFeedbackCallback: ((ExtendedUserFeedback) -> Unit)?,
    ) {
        telemetry.postUserFeedback(
            userFeedback.feedbackMetadata?.userFeedbackMetadata
                ?: telemetry.startBuildUserFeedbackMetadata().metadata,
            userFeedback.mapToNative(),
        ) { result ->
            result.fold({
                // TODO should we notify callback in case of error?
            }, { location ->
                // TODO why do we use -1 for id?
                val userFeedbackInternal = userFeedback.mapToInternal(
                    "-1",
                    location,
                )
                userFeedbackCallback?.invoke(userFeedbackInternal)
                userFeedbackObservers.forEach {
                    it.onNewUserFeedback(userFeedbackInternal)
                }
            },)
        }
    }

    fun postCustomEvent(type: String, version: String, payload: String?) {
        telemetry.postTelemetryCustomEvent(type, version, payload)
    }

    fun postAndroidAutoEvent(event: AndroidAutoEvent) {
        telemetry.postOuterDeviceEvent(event.mapToNative())
    }

    fun registerUserFeedbackObserver(observer: UserFeedbackObserver) {
        userFeedbackObservers.add(observer)
    }

    fun unregisterUserFeedbackObserver(observer: UserFeedbackObserver) {
        userFeedbackObservers.remove(observer)
    }

    fun clearObservers() {
        userFeedbackObservers.clear()
    }

    companion object {
        fun create(tripSession: TripSession, nativeNavigator: MapboxNativeNavigator) =
            NavigationTelemetry(tripSession, nativeNavigator)
    }
}
