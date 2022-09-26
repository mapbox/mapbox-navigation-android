package com.mapbox.navigation.dropin

/**
 * Params of a button that can be enabled.
 *
 * @param enabled true if it should be enabled, false otherwise
 * @param buttonParams [MapboxExtendableButtonParams] of the button
 */
class MapboxEnablableButtonParams(
    val enabled: Boolean,
    val buttonParams: MapboxExtendableButtonParams,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MapboxEnablableButtonParams

        if (enabled != other.enabled) return false
        if (buttonParams != other.buttonParams) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = enabled.hashCode()
        result = 31 * result + buttonParams.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "MapboxEnablableButtonParams(enabled=$enabled, buttonParams=$buttonParams)"
    }
}
