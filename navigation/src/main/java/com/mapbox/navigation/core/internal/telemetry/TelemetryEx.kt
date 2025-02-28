@file:Suppress("unused")
@file:OptIn(ExperimentalPreviewMapboxNavigationAPI::class)

package com.mapbox.navigation.core.internal.telemetry

import androidx.annotation.RestrictTo
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.telemetry.UserFeedback

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun MapboxNavigation.registerUserFeedbackObserver(observer: UserFeedbackObserver) {
    navigationTelemetry.registerUserFeedbackObserver(observer)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun MapboxNavigation.unregisterUserFeedbackObserver(observer: UserFeedbackObserver) {
    navigationTelemetry.unregisterUserFeedbackObserver(observer)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun MapboxNavigation.postUserFeedback(
    userFeedback: UserFeedback,
    callback: (ExtendedUserFeedback) -> Unit,
) {
    postUserFeedbackInternal(userFeedback, callback)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun MapboxNavigation.postAndroidAutoEvent(event: AndroidAutoEvent) {
    navigationTelemetry.postAndroidAutoEvent(event)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun MapboxNavigation.postCustomEvent(type: String, version: String, payload: String?) {
    navigationTelemetry.postCustomEvent(type, version, payload)
}
