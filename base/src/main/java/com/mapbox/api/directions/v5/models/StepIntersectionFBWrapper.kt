package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.FlatbuffersListWrapper
import com.mapbox.api.directions.v5.models.utils.throwNotComparableRouteObjects
import com.mapbox.api.directions.v5.models.utils.toDoubleArrayOrEmpty
import com.mapbox.api.directions.v5.models.utils.toHashCode
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.directions.accessors.FbBuffer
import com.mapbox.directions.accessors.FbRoadClassEnumWrapper
import com.mapbox.directions.accessors.FbStepIntersection
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class StepIntersectionFBWrapper private constructor(
    private val b: FbBuffer,
    private val fb: FbStepIntersection,
) : StepIntersection(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedProperties(b).asByteBuffer(b)

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedProperties(b).size

    override fun rawLocation(): DoubleArray {
        return fb.location(b).toDoubleArrayOrEmpty(b)
    }

    override fun bearings(): List<Int>? {
        val bearings = fb.bearings(b)
        return FlatbuffersListWrapper.get(bearings.size) {
            bearings.get(b, it)
        }
    }

    override fun classes(): List<String?>? {
        val classes = fb.classes(b)
        return FlatbuffersListWrapper.get(classes.size) {
            classes.get(b, it).toRoadClassString(b)
        }
    }

    override fun entry(): List<Boolean>? {
        val entry = fb.entry(b)
        return FlatbuffersListWrapper.get(entry.size) {
            entry.get(b, it)
        }
    }

    override fun formOfWay(): List<String?>? {
        val formOfWay = fb.formOfWay(b)
        return FlatbuffersListWrapper.get(formOfWay.size) {
            formOfWay.get(b, it)
        }
    }

    override fun geometries(): List<String?>? {
        val geometries = fb.geometries(b)
        return FlatbuffersListWrapper.get(geometries.size) {
            val geometry = geometries.get(b, it)
            when {
                !geometry.isPresent -> null
                geometry.isNull(b) -> null
                else -> geometry.value(b)
            }
        }
    }

    override fun access(): List<Int?>? {
        val access = fb.access(b)
        return FlatbuffersListWrapper.get(access.size) {
            access.get(b, it)
        }
    }

    override fun elevated(): List<Boolean?>? {
        val elevated = fb.elevated(b)
        return FlatbuffersListWrapper.get(elevated.size) {
            elevated.get(b, it)
        }
    }

    override fun bridges(): List<Boolean?>? {
        val bridges = fb.bridges(b)
        return FlatbuffersListWrapper.get(bridges.size) {
            bridges.get(b, it)
        }
    }

    override fun `in`(): Int? = fb.inIndexOrNull(b)

    override fun out(): Int? = fb.outIndexOrNull(b)

    override fun lanes(): List<IntersectionLanes?>? {
        val lanes = fb.lanes(b)
        return FlatbuffersListWrapper.get(lanes.size) {
            IntersectionLanesFBWrapper.wrap(b, lanes.get(b, it))
        }
    }

    override fun geometryIndex(): Int? = fb.geometryIndexOrNull(b)

    override fun isUrban(): Boolean? = fb.isUrbanOrNull(b)

    override fun adminIndex(): Int? = fb.adminIndexOrNull(b)

    override fun restStop(): RestStop? {
        return RestStopFBWrapper.wrap(b, fb.restStop(b))
    }

    override fun tollCollection(): TollCollection? {
        return TollCollectionFBWrapper.wrap(b, fb.tollCollection(b))
    }

    override fun mapboxStreetsV8(): MapboxStreetsV8? {
        return MapboxStreetsV8FBWrapper.wrap(b, fb.mapboxStreetsV8(b))
    }

    override fun tunnelName(): String? = fb.tunnelName(b)

    override fun railwayCrossing(): Boolean? = fb.railwayCrossingOrNull(b)

    override fun trafficSignal(): Boolean? = fb.trafficSignalOrNull(b)

    override fun stopSign(): Boolean? = fb.stopSignOrNull(b)

    override fun yieldSign(): Boolean? = fb.yieldSignOrNull(b)

    override fun interchange(): Interchange? {
        return InterchangeFBWrapper.wrap(b, fb.interchange(b))
    }

    override fun junction(): Junction? {
        return JunctionFBWrapper.wrap(b, fb.junction(b))
    }

    override fun mergingArea(): MergingArea? {
        return MergingAreaFBWrapper.wrap(b, fb.mergingArea(b))
    }

    override fun duration(): Double? = fb.durationOrNull(b)

    override fun toBuilder(): Builder? {
        NotSupportedForNativeRouteObject("StepIntersection#toBuilder()")
    }

    override fun unrecognized(): Map<String, SerializableJsonElement?>? {
        return super<BaseFBWrapper>.unrecognized()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (other is StepIntersection && other !is StepIntersectionFBWrapper) {
            throwNotComparableRouteObjects()
        }
        if (other !is StepIntersectionFBWrapper) return false
        return fb.contentEquals(b, other.fb, other.b)
    }

    override fun hashCode() = fb.hash(b).toHashCode()

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

    internal companion object {

        internal fun wrap(b: FbBuffer, fb: FbStepIntersection): StepIntersection? {
            return when {
                !fb.isPresent -> null
                fb.isNull(b) -> null
                else -> StepIntersectionFBWrapper(b, fb)
            }
        }

        private fun FbRoadClassEnumWrapper.toRoadClassString(b: FbBuffer): String? {
            return if (!isPresent || isNull(b)) {
                null
            } else {
                when (FBRoadClass.fromByteOrThrow(value(b))) {
                    FBRoadClass.Toll -> "toll"
                    FBRoadClass.Ferry -> "ferry"
                    FBRoadClass.Motorway -> "motorway"
                    FBRoadClass.Restricted -> "restricted"
                    FBRoadClass.Tunnel -> "tunnel"
                    FBRoadClass.Unknown -> unrecognizedValue(b)
                }
            }
        }
    }
}
