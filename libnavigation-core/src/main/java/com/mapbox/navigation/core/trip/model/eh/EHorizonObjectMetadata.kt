package com.mapbox.navigation.core.trip.model.eh

import com.mapbox.navigation.base.trip.model.alert.IncidentInfo

/**
 * RoadObject metadata
 *
 * @param type type of road object
 * @param objectProvider provider of road object
 * @param incidentInfo will be filled only if `type` is `Incident` and `objectProvider` is `Mapbox`
 */
class EHorizonObjectMetadata internal constructor(
    @EHorizonObjectType.Type val type: String,
    @EHorizonObjectProvider.Type val objectProvider: String,
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
