package com.mapbox.navigation.core.history.model

/**
 * Represents a historical status event.
 *
 * @param eventTimestamp timestamp of event seconds
 * @param elapsedRealtimeNanos monotonic timestamp for when this event was recorded
 */
class HistoryEventGetStatus internal constructor(
    override val eventTimestamp: Double,
    val elapsedRealtimeNanos: Long,
) : HistoryEvent {

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HistoryEventGetStatus

        if (elapsedRealtimeNanos != other.elapsedRealtimeNanos) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        return elapsedRealtimeNanos.hashCode()
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "GetStatusHistoryEvent(" +
            "elapsedRealtimeNanos=$elapsedRealtimeNanos" +
            ")"
    }
}
