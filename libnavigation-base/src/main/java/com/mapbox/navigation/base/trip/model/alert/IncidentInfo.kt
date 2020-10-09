package com.mapbox.navigation.base.trip.model.alert

import java.util.Date

/**
 * Additional info of [IncidentAlert].
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
class IncidentInfo private constructor(
    val id: String,
    val type: Int,
    @IncidentImpact.Impact val impact: String?,
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
     * Transform this object into a builder to mutate the values.
     */
    fun toBuilder(): Builder = Builder(id)
        .type(type)
        .impact(impact)
        .congestion(congestion)
        .isClosed(isClosed)
        .creationTime(creationTime)
        .startTime(startTime)
        .endTime(endTime)
        .description(description)
        .subType(subType)
        .subTypeDescription(subTypeDescription)
        .alertcCodes(alertcCodes)

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

    /**
     * Use to create a new instance.
     *
     * @see IncidentAlert
     */
    class Builder(
        private val id: String,
    ) {

        private var type: Int = IncidentType.UNKNOWN

        @IncidentImpact.Impact
        private var impact: String? = null
        private var congestion: IncidentCongestion? = null
        private var isClosed: Boolean = false
        private var creationTime: Date? = null
        private var startTime: Date? = null
        private var endTime: Date? = null
        private var description: String? = null
        private var subType: String? = null
        private var subTypeDescription: String? = null
        private var alertcCodes: List<Int>? = null

        /**
         * One of [IncidentType].
         */
        fun type(incidentType: Int): Builder = apply {
            this.type = incidentType
        }

        /**
         * Severity level of incident.
         */
        fun impact(@IncidentImpact.Impact impact: String?): Builder = apply {
            this.impact = impact
        }

        /**
         * Quantitative descriptor of congestion.
         */
        fun congestion(congestion: IncidentCongestion?): Builder = apply {
            this.congestion = congestion
        }

        /**
         * **True** if road is closed and no possibility to pass through there. **False**
         * otherwise.
         */
        fun isClosed(isClosed: Boolean): Builder = apply {
            this.isClosed = isClosed
        }

        /**
         * Time the incident was created/updated in ISO8601 format. The incident alert can be
         * created/updated at a different time than its occurrence.
         */
        fun creationTime(creationTime: Date?): Builder = apply {
            this.creationTime = creationTime
        }

        /**
         * Start time of the incident in ISO8601 format.
         */
        fun startTime(startTime: Date?): Builder = apply {
            this.startTime = startTime
        }

        /**
         * End time of the incident in ISO8601 format.
         */
        fun endTime(endTime: Date?): Builder = apply {
            this.endTime = endTime
        }

        /**
         * Human-readable description of the incident suitable for displaying to the users.
         */
        fun description(description: String?): Builder = apply {
            this.description = description
        }

        /**
         * Sub-type of the incident.
         */
        fun subType(subType: String?): Builder = apply {
            this.subType = subType
        }

        /**
         * Sub-type-specific description.
         */
        fun subTypeDescription(subTypeDescription: String?): Builder = apply {
            this.subTypeDescription = subTypeDescription
        }

        /**
         * AlertC codes.
         *
         * @see <a href="https://www.iso.org/standard/59231.html">AlertC</a>
         */
        fun alertcCodes(alertcCodes: List<Int>?): Builder = apply {
            this.alertcCodes = alertcCodes
        }

        /**
         * Build the object instance.
         */
        fun build(): IncidentInfo =
            IncidentInfo(
                id,
                type,
                impact,
                congestion,
                isClosed,
                creationTime,
                startTime,
                endTime,
                description,
                subType,
                subTypeDescription,
                alertcCodes
            )
    }
}
