package com.mapbox.navigation.core.trip.model.eh

import com.mapbox.navigation.base.trip.model.alert.IncidentInfo

class EHorizonObjectMetadata internal constructor(
    val type: String,
    val objectProvider: String,
    val incidentInfo: IncidentInfo?,
) {

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EHorizonObjectMetadata

        if (type != other.type) return false
        if (objectProvider != other.objectProvider) return false
        if (incidentInfo != other.incidentInfo) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + objectProvider.hashCode()
        result = 31 * result + (incidentInfo?.hashCode() ?: 0)
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "EHorizonObjectMetadata(" +
            "type=$type, " +
            "objectProvider='$objectProvider', " +
            "incidentInfo=$incidentInfo)"
    }
}
