package com.mapbox.navigation.core.history.model

/**
 * Allows to read custom events with **type**-**properties** structure
 *
 * @param type
 * @param properties
 */
// fixme add docs when expose [HistoryRecorderHandle#pushHistory]
class HistoryEventPushHistoryRecord internal constructor(
    val type: String,
    val properties: String
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
