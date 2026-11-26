package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.FlatbuffersListWrapper
import com.mapbox.api.directions.v5.models.utils.throwNotComparableRouteObjects
import com.mapbox.api.directions.v5.models.utils.toHashCode
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.directions.accessors.FbBuffer
import com.mapbox.directions.accessors.FbIncident
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class IncidentFBWrapper private constructor(
    private val b: FbBuffer,
    private val fb: FbIncident,
) : Incident(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedProperties(b).asByteBuffer(b)

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedProperties(b).size

    override fun id(): String = fb.id(b)

    override fun type(): String? {
        return when (FBIncidentType.fromByteOrThrow(fb.type(b))) {
            FBIncidentType.Accident -> Incident.INCIDENT_ACCIDENT
            FBIncidentType.Congestion -> Incident.INCIDENT_CONGESTION
            FBIncidentType.Construction -> Incident.INCIDENT_CONSTRUCTION
            FBIncidentType.DisabledVehicle -> Incident.INCIDENT_DISABLED_VEHICLE
            FBIncidentType.LaneRestriction -> Incident.INCIDENT_LANE_RESTRICTION
            FBIncidentType.MassTransit -> Incident.INCIDENT_MASS_TRANSIT
            FBIncidentType.Miscellaneous -> Incident.INCIDENT_MISCELLANEOUS
            FBIncidentType.OtherNews -> Incident.INCIDENT_OTHER_NEWS
            FBIncidentType.PlannedEvent -> Incident.INCIDENT_PLANNED_EVENT
            FBIncidentType.RoadClosure -> Incident.INCIDENT_ROAD_CLOSURE
            FBIncidentType.RoadHazard -> Incident.INCIDENT_ROAD_HAZARD
            FBIncidentType.Weather -> Incident.INCIDENT_WEATHER
            FBIncidentType.Unknown -> unrecognizeFlexBufferMap?.get("type")?.asString()
        }
    }

    override fun closed(): Boolean? = fb.closedOrNull(b)

    override fun congestion(): Congestion? {
        return CongestionFBWrapper.wrap(b, fb.congestion(b))
    }

    override fun description(): String? = fb.description(b)

    override fun longDescription(): String? = fb.longDescription(b)

    override fun impact(): String? {
        return when (FBIncidentImpact.fromByteOrThrow(fb.impactOrNull(b) ?: return null)) {
            FBIncidentImpact.Critical -> Incident.IMPACT_CRITICAL
            FBIncidentImpact.Major -> Incident.IMPACT_MAJOR
            FBIncidentImpact.Minor -> Incident.IMPACT_MINOR
            FBIncidentImpact.Low -> Incident.IMPACT_LOW
            FBIncidentImpact.UnknownImpact -> Incident.IMPACT_UNKNOWN
            FBIncidentImpact.Unknown -> unrecognizeFlexBufferMap?.get("impact")?.asString()
        }
    }

    override fun subType(): String? = fb.subType(b)

    override fun subTypeDescription(): String? = fb.subTypeDescription(b)

    override fun alertcCodes(): List<Int>? {
        val alertcCodes = fb.alertcCodes(b)
        return FlatbuffersListWrapper.get(alertcCodes.size) {
            alertcCodes.get(b, it)
        }
    }

    override fun trafficCodes(): TrafficCodes? {
        return TrafficCodesFBWrapper.wrap(b, fb.trafficCodes(b))
    }

    override fun geometryIndexStart(): Int? = fb.geometryIndexStartOrNull(b)

    override fun geometryIndexEnd(): Int? = fb.geometryIndexEndOrNull(b)

    override fun creationTime(): String? = fb.creationTime(b)

    override fun startTime(): String? = fb.startTime(b)

    override fun endTime(): String? = fb.endTime(b)

    override fun countryCodeAlpha2(): String? = fb.countryCodeAlpha2(b)

    override fun countryCodeAlpha3(): String? = fb.countryCodeAlpha3(b)

    override fun lanesBlocked(): List<String>? {
        val lanesBlocked = fb.lanesBlocked(b)
        return FlatbuffersListWrapper.get(lanesBlocked.size) {
            lanesBlocked.get(b, it)
        }?.filterNotNull()
    }

    override fun numLanesBlocked(): Int? = fb.numLanesBlockedOrNull(b)

    override fun affectedRoadNames(): List<String>? {
        val affectedRoadNames = fb.affectedRoadNames(b)
        return FlatbuffersListWrapper.get(affectedRoadNames.size) {
            affectedRoadNames.get(b, it)
        }?.filterNotNull()
    }

    override fun toBuilder(): Builder? {
        NotSupportedForNativeRouteObject("Incident#toBuilder()")
    }

    override fun unrecognized(): Map<String, SerializableJsonElement?>? {
        return super<BaseFBWrapper>.unrecognized()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (other is Incident && other !is IncidentFBWrapper) {
            throwNotComparableRouteObjects()
        }
        if (other !is IncidentFBWrapper) return false
        return fb.contentEquals(b, other.fb, other.b)
    }

    override fun hashCode() = fb.hash(b).toHashCode()

    override fun toString(): String {
        return "Incident(" +
            "id=${id()}, " +
            "type=${type()}, " +
            "closed=${closed()}, " +
            "congestion=${congestion()}, " +
            "description=${description()}, " +
            "longDescription=${longDescription()}, " +
            "impact=${impact()}, " +
            "subType=${subType()}, " +
            "subTypeDescription=${subTypeDescription()}, " +
            "alertcCodes=${alertcCodes()}, " +
            "trafficCodes=${trafficCodes()}, " +
            "geometryIndexStart=${geometryIndexStart()}, " +
            "geometryIndexEnd=${geometryIndexEnd()}, " +
            "creationTime=${creationTime()}, " +
            "startTime=${startTime()}, " +
            "endTime=${endTime()}, " +
            "countryCodeAlpha2=${countryCodeAlpha2()}, " +
            "countryCodeAlpha3=${countryCodeAlpha3()}, " +
            "lanesBlocked=${lanesBlocked()}, " +
            "numLanesBlocked=${numLanesBlocked()}, " +
            "affectedRoadNames=${affectedRoadNames()}" +
            ")"
    }

    internal companion object {
        internal fun wrap(b: FbBuffer, fb: FbIncident): Incident? {
            return when {
                !fb.isPresent -> null
                fb.isNull(b) -> null
                else -> IncidentFBWrapper(b, fb)
            }
        }
    }
}
