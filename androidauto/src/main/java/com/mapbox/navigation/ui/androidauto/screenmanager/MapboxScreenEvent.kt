package com.mapbox.navigation.ui.androidauto.screenmanager

/**
 * This class is observable but must be created by the [MapboxScreenManager].
 *
 * Whenever a screen is changed with [MapboxScreenManager] an event is triggered.
 * This can be used to observe and control the car screen from the mobile device.
 */
class MapboxScreenEvent internal constructor(
    @MapboxScreen.Key
    val key: String,
    @MapboxScreenOperation.Type
    val operation: String,
) {

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MapboxScreenEvent

        if (key != other.key) return false
        if (operation != other.operation) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + operation.hashCode()
        return result
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun toString(): String {
        return "MapboxScreenEvent(key='$key', operation=$operation)"
    }
}
