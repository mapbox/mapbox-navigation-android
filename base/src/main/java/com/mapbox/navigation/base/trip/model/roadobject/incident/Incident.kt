package com.mapbox.navigation.base.trip.model.roadobject.incident

import com.mapbox.navigation.base.trip.model.roadobject.RoadObject
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectProvider
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectType

/**
 * Incident alert provides information about incidents on a way like *congestion*, *mass transit*,
 * etc. More types of incidents see [IncidentType].
 *
 * @param info incident alert info.
 */
class Incident internal constructor(
    id: String,
    val info: IncidentInfo,
    length: Double?,
    @RoadObjectProvider.Type provider: String,
    isUrban: Boolean?,
    nativeRoadObject: com.mapbox.navigator.RoadObject,
) : RoadObject(
    id,
    RoadObjectType.INCIDENT,
    length,
    provider,
    isUrban,
    nativeRoadObject,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as Incident

        if (info != other.info) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + info.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "Incident(" +
            "info=$info" +
            "), ${super.toString()}"
    }
}
