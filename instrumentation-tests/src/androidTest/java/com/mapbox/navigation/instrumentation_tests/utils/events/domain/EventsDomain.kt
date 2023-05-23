package com.mapbox.navigation.instrumentation_tests.utils.events.domain

internal abstract class EventBase(
    driverMode: DriverMode,
) {
    abstract val event: String
    val eventVersion: Double = 2.4
    val driverMode: String = when (driverMode) {
        DriverMode.FreeDrive -> "freeDrive"
        DriverMode.ActiveGuidance -> "trip"
    }

    enum class DriverMode {
        FreeDrive,
        ActiveGuidance,
    }
}

internal abstract class EventBaseActiveGuidance(

) : EventBase(DriverMode.ActiveGuidance) {

}

internal class EventFeedback(
    driverMode: DriverMode,
    val feedbackType: String,
    val description: String,
    val feedbackSubType: Array<String>,
    val screenshot: String,
): EventBase(driverMode) {
    override val event: String = "navigation.feedback"
}

internal class EventFreeDrive(
    eventType: Type,
) : EventBase(DriverMode.FreeDrive) {
    override val event: String = "navigation.freeDrive"
    val eventType: String = when (eventType) {
        Type.Start -> "start"
        Type.Stop -> "stop"
    }

    enum class Type {
        Start,
        Stop,
    }
}

internal class EventNavigationStateChanged(
    state: State,
) : EventBase(DriverMode.ActiveGuidance) {
    override val event: String = "navigation.navigationStateChanged"
    val state: String = when (state) {
        State.NavStarted -> "navigation_started"
        State.NavEnded -> "navigation_ended"
    }

    enum class State {
        NavStarted,
        NavEnded,
    }
}

internal class EventDepart : EventBaseActiveGuidance() {
    override val event: String = "navigation.depart"
}

internal class EventArrive : EventBaseActiveGuidance() {
    override val event: String = "navigation.arrive"
}

/**
 * "volumeLevel" -> {Value@20958} "33"
"driverMode" -> {Value@20960} "freeDrive"
"batteryPluggedIn" -> {Value@20962} "false"
"eventVersion" -> {Value@20964} "2.4"
"simulation" -> {Value@20966} "true"
"audioType" -> {Value@20968} "headphones"
"percentTimeInPortrait" -> {Value@20970} "100"
"operatingSystem" -> {Value@20972} "Android 11"
"driverModeId" -> {Value@20974} "6283cc3d-6262-4833-86f5-663581327e2f"
"connectivity" -> {Value@20976} "Unknown"
"driverModeStartTimestamp" -> {Value@20978} "2023-05-17T16:35:26.530Z"
"percentTimeInForeground" -> {Value@20980} "100"
"driverModeStartTimestampMonotime" -> {Value@20982} "526084002909708"
"event" -> {Value@20984} "navigation.freeDrive"
"lat" -> {Value@20986} "38.89514907122847"
"batteryLevel" -> {Value@20988} "100"
"lng" -> {Value@20990} "-77.03195362937024"
"created" -> {Value@20992} "2023-05-17T16:35:33.859Z"
"eventType" -> {Value@20994} "stop"
"version" -> {Value@20996} "2.4"
"sdkIdentifier" -> {Value@20998} "mapbox-navigation-android"
"createdMonotime" -> {Value@21000} "526091271247708"
"locationEngine" -> {Value@21002} "fused"
"screenBrightness" -> {Value@21004} "40"
"device" -> {Value@21006} "generic_x86 (x86; Android SDK built for x86)"
 */
