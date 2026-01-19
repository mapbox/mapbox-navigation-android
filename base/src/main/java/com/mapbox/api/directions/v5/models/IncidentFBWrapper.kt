package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.FlatbuffersListWrapper
import com.mapbox.api.directions.v5.models.utils.unhandledEnumMapping
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class IncidentFBWrapper(
    private val fb: FBIncident,
) : Incident(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedPropertiesAsByteBuffer

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedPropertiesLength

    override fun id(): String = fb.id

    override fun type(): String? {
        return when (fb.type) {
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
            else -> unhandledEnumMapping("type", fb.type)
        }
    }

    override fun closed(): Boolean? = fb.closed

    override fun congestion(): Congestion? {
        return fb.congestion?.let { CongestionFBWrapper(it) }
    }

    override fun description(): String? = fb.description

    override fun longDescription(): String? = fb.longDescription

    override fun impact(): String? {
        val impact = fb.impact ?: return null
        return when (impact) {
            FBIncidentImpact.Critical -> Incident.IMPACT_CRITICAL
            FBIncidentImpact.Major -> Incident.IMPACT_MAJOR
            FBIncidentImpact.Minor -> Incident.IMPACT_MINOR
            FBIncidentImpact.Low -> Incident.IMPACT_LOW
            FBIncidentImpact.Unknown -> unrecognizeFlexBufferMap?.get("impact")?.asString()
            else -> unhandledEnumMapping("impact", fb.impact)
        }
    }

    override fun subType(): String? = fb.subType

    override fun subTypeDescription(): String? = fb.subTypeDescription

    override fun alertcCodes(): List<Int>? {
        return FlatbuffersListWrapper.get(fb.alertcCodesLength) {
            fb.alertcCodes(it)
        }
    }

    override fun trafficCodes(): TrafficCodes? {
        return fb.trafficCodes?.let { TrafficCodesFBWrapper(it) }
    }

    override fun geometryIndexStart(): Int? = fb.geometryIndexStart

    override fun geometryIndexEnd(): Int? = fb.geometryIndexEnd

    override fun creationTime(): String? = fb.creationTime

    override fun startTime(): String? = fb.startTime

    override fun endTime(): String? = fb.endTime

    override fun countryCodeAlpha2(): String? = fb.countryCodeAlpha2

    override fun countryCodeAlpha3(): String? = fb.countryCodeAlpha3

    override fun lanesBlocked(): List<String>? {
        return FlatbuffersListWrapper.get(fb.lanesBlockedLength) {
            fb.lanesBlocked(it)
        }?.filterNotNull()
    }

    override fun numLanesBlocked(): Int? = fb.numLanesBlocked

    override fun affectedRoadNames(): List<String>? {
        return FlatbuffersListWrapper.get(fb.affectedRoadNamesLength) {
            fb.affectedRoadNames(it)
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
        if (other is IncidentFBWrapper && other.fb === fb) return true
        if (other is IncidentFBWrapper && efficientEquals(fb, other.fb)) return true

        return false
    }

    override fun hashCode(): Int {
        return efficientHashCode(fb)
    }

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
}
