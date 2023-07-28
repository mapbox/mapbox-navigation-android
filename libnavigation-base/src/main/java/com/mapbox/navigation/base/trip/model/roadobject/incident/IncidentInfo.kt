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
 * @param trafficCodes map of traffic code names to their values.
 *  For example, the map may contain info about jartic_regulation_code and jartic_cause_code.
 * @param countryCodeAlpha2 ISO 3166-1, 2 letter country code.
 * @param countryCodeAlpha3 ISO 3166-1, 3 letter country code.
 * @param lanesBlocked lanes which are blocked. Might be: LEFT, LEFT CENTER, LEFT TURN LANE, CENTER,
 * RIGHT, RIGHT CENTER, RIGHT TURN LANE, HOV, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, SIDE, SHOULDER, MEDIAN.
 * @param longDescription detailed description of the incident.
 * @param lanesClearDesc describes which lanes are clear.
 *  For Example:
 *  - one lane gets by
 *  - only shoulder gets by
 *  - two left lanes get by
 * @param numLanesBlocked number of lanes blocked.
 * @param affectedRoadNames affected road names.
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
    val alertcCodes: List<Int>?,
    val trafficCodes: Map<String, Int>,
    val countryCodeAlpha2: String?,
    val countryCodeAlpha3: String?,
    val lanesBlocked: List<String>,
    val longDescription: String?,
    val lanesClearDesc: String?,
    val numLanesBlocked: Long?,
    val affectedRoadNames: List<String>?,
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
        if (trafficCodes != other.trafficCodes) return false
        if (countryCodeAlpha2 != other.countryCodeAlpha2) return false
        if (countryCodeAlpha3 != other.countryCodeAlpha3) return false
        if (lanesBlocked != other.lanesBlocked) return false
        if (longDescription != other.longDescription) return false
        if (lanesClearDesc != other.lanesClearDesc) return false
        if (numLanesBlocked != other.numLanesBlocked) return false
        if (affectedRoadNames != other.affectedRoadNames) return false

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
        result = 31 * result + trafficCodes.hashCode()
        result = 31 * result + countryCodeAlpha2.hashCode()
        result = 31 * result + countryCodeAlpha3.hashCode()
        result = 31 * result + lanesBlocked.hashCode()
        result = 31 * result + longDescription.hashCode()
        result = 31 * result + lanesClearDesc.hashCode()
        result = 31 * result + numLanesBlocked.hashCode()
        result = 31 * result + affectedRoadNames.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "IncidentInfo(" +
            "id='$id', " +
            "type=$type, " +
            "impact='$impact', " +
            "congestion=$congestion, " +
            "isClosed=$isClosed, " +
            "creationTime=$creationTime, " +
            "startTime=$startTime, " +
            "endTime=$endTime, " +
            "description=$description, " +
            "subType=$subType, " +
            "subTypeDescription=$subTypeDescription, " +
            "alertcCodes=$alertcCodes, " +
            "trafficCodes=$trafficCodes, " +
            "countryCodeAlpha2=$countryCodeAlpha2, " +
            "countryCodeAlpha3=$countryCodeAlpha3, " +
            "lanesBlocked=$lanesBlocked, " +
            "longDescription=$longDescription, " +
            "lanesClearDesc=$lanesClearDesc, " +
            "numLanesBlocked=$numLanesBlocked, " +
            "affectedRoadNames=$affectedRoadNames" +
            ")"
    }
}
