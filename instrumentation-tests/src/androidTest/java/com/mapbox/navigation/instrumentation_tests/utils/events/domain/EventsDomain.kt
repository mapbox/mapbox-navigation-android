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

internal class EventCancel : EventBaseActiveGuidance() {
    override val event: String = "navigation.cancel"
}

internal class EventAlternativeRoute: EventBaseActiveGuidance() {
    override val event: String = "navigation.alternativeRoute"
}

internal class EventReroute: EventBaseActiveGuidance() {
    override val event: String = "navigation.reroute"
}
