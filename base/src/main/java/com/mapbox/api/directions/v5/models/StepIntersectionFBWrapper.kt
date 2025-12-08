package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.FlatbuffersListWrapper
import com.mapbox.api.directions.v5.models.utils.toDoubleArrayOrEmpty
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class StepIntersectionFBWrapper(
    private val fb: FBStepIntersection,
) : StepIntersection(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedPropertiesAsByteBuffer

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedPropertiesLength

    override fun rawLocation(): DoubleArray {
        return fb.location.toDoubleArrayOrEmpty()
    }

    override fun bearings(): List<Int>? {
        return FlatbuffersListWrapper.get(fb.bearingsLength) {
            fb.bearings(it)
        }
    }

    override fun classes(): List<String?>? {
        return FlatbuffersListWrapper.get(fb.classesLength) {
            fb.classes(it)?.toRoadClassString()
        }
    }

    override fun entry(): List<Boolean>? {
        return FlatbuffersListWrapper.get(fb.entryLength) {
            fb.entry(it)
        }
    }

    override fun formOfWay(): List<String?>? {
        return FlatbuffersListWrapper.get(fb.formOfWayLength) {
            fb.formOfWay(it)
        }
    }

    override fun geometries(): List<String?>? {
        return FlatbuffersListWrapper.get(fb.geometriesLength) {
            fb.geometries(it)?.value
        }
    }

    override fun access(): List<Int?>? {
        return FlatbuffersListWrapper.get(fb.accessLength) {
            fb.access(it)
        }
    }

    override fun elevated(): List<Boolean?>? {
        return FlatbuffersListWrapper.get(fb.elevatedLength) {
            fb.elevated(it)
        }
    }

    override fun bridges(): List<Boolean?>? {
        return FlatbuffersListWrapper.get(fb.bridgesLength) {
            fb.bridges(it)
        }
    }

    override fun `in`(): Int? = fb.inIndex

    override fun out(): Int? = fb.outIndex

    override fun lanes(): List<IntersectionLanes?>? {
        return FlatbuffersListWrapper.get(fb.lanesLength) {
            fb.lanes(it)?.let { IntersectionLanesFBWrapper(it) }
        }
    }

    override fun geometryIndex(): Int? = fb.geometryIndex

    override fun isUrban(): Boolean? = fb.isUrban

    override fun adminIndex(): Int? = fb.adminIndex

    override fun restStop(): RestStop? {
        return fb.restStop?.let { RestStopFBWrapper(it) }
    }
    override fun tollCollection(): TollCollection? {
        return fb.tollCollection?.let { TollCollectionFBWrapper(it) }
    }

    override fun mapboxStreetsV8(): MapboxStreetsV8? {
        return fb.mapboxStreetsV8?.let { MapboxStreetsV8FBWrapper(it) }
    }
    override fun tunnelName(): String? = fb.tunnelName

    override fun railwayCrossing(): Boolean? = fb.railwayCrossing

    override fun trafficSignal(): Boolean? = fb.trafficSignal

    override fun stopSign(): Boolean? = fb.stopSign

    override fun yieldSign(): Boolean? = fb.yieldSign

    override fun interchange(): Interchange? {
        return fb.interchange?.let { InterchangeFBWrapper(it) }
    }

    override fun junction(): Junction? {
        return fb.junction?.let { JunctionFBWrapper(it) }
    }
    override fun mergingArea(): MergingArea? {
        return fb.mergingArea?.let { MergingAreaFBWrapper(it) }
    }

    override fun duration(): Double? = fb.duration

    override fun toBuilder(): Builder? {
        NotSupportedForNativeRouteObject("StepIntersection#toBuilder()")
    }

    override fun unrecognized(): Map<String, SerializableJsonElement?>? {
        return super<BaseFBWrapper>.unrecognized()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is StepIntersectionFBWrapper && other.fb === fb) return true
        if (other is StepIntersectionFBWrapper && efficientEquals(fb, other.fb)) return true

        return false
    }

    override fun hashCode(): Int {
        return efficientHashCode(fb)
    }

    override fun toString(): String {
        return "StepIntersection(" +
            "rawLocation=${rawLocation().contentToString()}, " +
            "bearings=${bearings()}, " +
            "classes=${classes()}, " +
            "entry=${entry()}, " +
            "formOfWay=${formOfWay()}, " +
            "geometries=${geometries()}, " +
            "access=${access()}, " +
            "elevated=${elevated()}, " +
            "bridges=${bridges()}, " +
            "in=${`in`()}, " +
            "out=${out()}, " +
            "lanes=${lanes()}, " +
            "geometryIndex=${geometryIndex()}, " +
            "isUrban=${isUrban()}, " +
            "adminIndex=${adminIndex()}, " +
            "restStop=${restStop()}, " +
            "tollCollection=${tollCollection()}, " +
            "mapboxStreetsV8=${mapboxStreetsV8()}, " +
            "tunnelName=${tunnelName()}, " +
            "railwayCrossing=${railwayCrossing()}, " +
            "trafficSignal=${trafficSignal()}, " +
            "stopSign=${stopSign()}, " +
            "yieldSign=${yieldSign()}, " +
            "interchange=${interchange()}, " +
            "junction=${junction()}, " +
            "mergingArea=${mergingArea()}, " +
            "duration=${duration()}" +
            ")"
    }

    private companion object {
        fun FBRoadClassEnumWrapper.toRoadClassString(): String? = when (this.value) {
            FBRoadClass.Toll -> "toll"
            FBRoadClass.Ferry -> "ferry"
            FBRoadClass.Motorway -> "motorway"
            FBRoadClass.Restricted -> "restricted"
            FBRoadClass.Tunnel -> "tunnel"
            else -> this.unrecognizedValue
        }
    }
}
