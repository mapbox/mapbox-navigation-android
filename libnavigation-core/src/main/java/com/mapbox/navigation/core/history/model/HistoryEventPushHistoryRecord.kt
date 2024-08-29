package com.mapbox.navigation.core.history.model

/**
 * Custom event from the navigators history.
 *
 * @param eventTimestamp timestamp of event seconds
 * @param type the event type in the events log
 * @param properties the json with properties of the event
 */
class HistoryEventPushHistoryRecord internal constructor(
    override val eventTimestamp: Double,
    val type: String,
    val properties: String,
) : HistoryEvent {

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HistoryEventPushHistoryRecord

        if (type != other.type) return false
        if (properties != other.properties) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + properties.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "HistoryEventPushHistoryRecord(" +
            "type=$type, " +
            "properties=$properties" +
            ")"
    }
}
