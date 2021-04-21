package com.mapbox.navigation.base.trip.model.roadobject.incident

import com.mapbox.api.directions.v5.models.Incident
import java.util.Date

/**
 * Additional info of [Incident].
 *
 * @param id unique id of incident.
 * @param type type of incident. For more information [IncidentType].
 * @param impact the rate of incident. For more information [IncidentImpact].
 * @param congestion quantitative descriptor of congestion.
 * @param isClosed **true** if road is closed and no possibility to pass through there. **False**
 * otherwise.
 * @param creationTime time the incident was created/updated in ISO8601 format. The incident
 * alert can be created/updated at a different time than its occurrence.
 * @param startTime start time of the incident in ISO8601 format.
 * @param endTime end time of the incident in ISO8601 format.
 * @param description human-readable description of the incident suitable for displaying to the
 * users.
 * @param subType sub-type of the incident.
 * @param subTypeDescription sub-type-specific description.
 * @param alertcCodes alertC codes.
 */
class IncidentInfo internal constructor(
    val id: String,
    @Incident.IncidentType val type: Int,
    @IncidentImpact.Impact val impact: String,
    val congestion: IncidentCongestion?,
    val isClosed: Boolean,
    val creationTime: Date?,
    val startTime: Date?,
    val endTime: Date?,
    val description: String?,
    val subType: String?,
    val subTypeDescription: String?,
    val alertcCodes: List<Int>?
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IncidentInfo

        if (type != other.type) return false
        if (id != other.id) return false
        if (impact != other.impact) return false
        if (congestion != other.congestion) return false
        if (isClosed != other.isClosed) return false
        if (creationTime != other.creationTime) return false
        if (startTime != other.startTime) return false
        if (endTime != other.endTime) return false
        if (description != other.description) return false
        if (subType != other.subType) return false
        if (subTypeDescription != other.subTypeDescription) return false
        if (alertcCodes != other.alertcCodes) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + impact.hashCode()
        result = 31 * result + congestion.hashCode()
        result = 31 * result + isClosed.hashCode()
        result = 31 * result + creationTime.hashCode()
        result = 31 * result + startTime.hashCode()
        result = 31 * result + endTime.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + subType.hashCode()
        result = 31 * result + subTypeDescription.hashCode()
        result = 31 * result + alertcCodes.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "IncidentInfo(" +
            "id=$id" +
            "type=$type" +
            "impact=$impact" +
            "congestion=$congestion" +
            "isClosed=$isClosed" +
            "creationTime=$creationTime" +
            "startTime=$startTime" +
            "endTime=$endTime" +
            "description=$description" +
            "subType=$subType" +
            "subTypeDescription=$subTypeDescription" +
            "alertcCodes=$alertcCodes" +
            ")"
    }
}
