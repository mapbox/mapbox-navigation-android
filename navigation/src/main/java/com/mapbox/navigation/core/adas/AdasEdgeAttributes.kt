package com.mapbox.navigation.core.adas

import androidx.annotation.IntDef
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Edge Adas Attributes
 *
 * @param speedLimit List of speed limits on the edge. Empty means no speed-limit data for the edge.
 * Multiple values will have different conditions.
 * @param slopes List of slope values with their positions on the edge.
 * Position is a shape index, where integer part in an index of geometry segment is
 * and fractional part is a position on the segment. Value is a slope in degrees
 * @param elevations List of elevation values with their positions on the edge.
 * Position is a shape index, where integer part in an index of geometry segment is
 * and fractional part is a position on the segment. Value is an elevation in meters above sea level.
 * @param curvatures List of curvature values with their positions on the edge.
 * Position is a shape index, where integer part in an index of geometry segment is
 * and fractional part is a position on the segment
 * @param isDividedRoad A flag indicating if the edge is a divided road
 * @param formOfWay Form Of Way information from ADAS tiles, may differ from the Valhalla value, but should be used for ADAS purposes.
 *  See [FormOfWay.Type]. If not set, then the value is not known
 * @param etc2 Road class in ETC2.0 format (Japan specific), see [Etc2Road.Type]
 */
@ExperimentalPreviewMapboxNavigationAPI
class AdasEdgeAttributes private constructor(
    val speedLimit: List<AdasSpeedLimitInfo>,
    val slopes: List<AdasValueOnEdge>,
    val elevations: List<AdasValueOnEdge>,
    val curvatures: List<AdasValueOnEdge>,
    val isDividedRoad: Boolean?,
    @FormOfWay.Type val formOfWay: Int?,
    @Etc2Road.Type val etc2: Int,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdasEdgeAttributes

        if (speedLimit != other.speedLimit) return false
        if (slopes != other.slopes) return false
        if (curvatures != other.curvatures) return false
        if (elevations != other.elevations) return false
        if (isDividedRoad != other.isDividedRoad) return false
        if (formOfWay != other.formOfWay) return false
        if (etc2 != other.etc2) return false
        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = speedLimit.hashCode()
        result = 31 * result + slopes.hashCode()
        result = 31 * result + elevations.hashCode()
        result = 31 * result + curvatures.hashCode()
        result = 31 * result + isDividedRoad.hashCode()
        result = 31 * result + formOfWay.hashCode()
        result = 31 * result + etc2.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "EdgeAdasAttributes(" +
            "speedLimit=$speedLimit, " +
            "slopes=$slopes, " +
            "elevations=$elevations, " +
            "curvatures=$curvatures, " +
            "isDividedRoad=$isDividedRoad, " +
            "formOfWay=$formOfWay, " +
            "etc2=$etc2" +
            ")"
    }

    /**
     * Form of way type.
     */
    object FormOfWay {

        /**
         * Unknown form of way.
         */
        const val UNKNOWN = 0

        /**
         * Freeway form of way.
         */
        const val FREEWAY = 1

        /**
         * Multiple carriageway form of way.
         */
        const val MULTIPLE_CARRIAGEWAY = 2

        /**
         * Single carriageway form of way.
         */
        const val SINGLE_CARRIAGEWAY = 3

        /**
         * Roundabout circle form of way.
         */
        const val ROUNDABOUT_CIRCLE = 4

        /**
         * Traffic square form of way.
         */
        const val TRAFFIC_SQUARE = 5

        /**
         * Slip road form of way.
         */
        const val SLIP_ROAD = 6

        /**
         * Parallel road form of way.
         */
        const val PARALLEL_ROAD = 7

        /**
         * Ramp on freeway form of way.
         */
        const val RAMP_ON_FREEWAY = 8

        /**
         * Ramp form of way.
         */
        const val RAMP = 9

        /**
         * Service road form of way.
         */
        const val SERVICE_ROAD = 10

        /**
         * Car park entrance form of way.
         */
        const val CAR_PARK_ENTRANCE = 11

        /**
         * Service entrance form of way.
         */
        const val SERVICE_ENTRANCE = 12

        /**
         * Pedestrian zone form of way.
         */
        const val PEDESTRIAN_ZONE = 13

        /**
         * Form of way is not applicable.
         */
        const val NA = 14

        /**
         * Retention policy for [FormOfWay].
         */
        @Retention(AnnotationRetention.BINARY)
        @IntDef(
            UNKNOWN,
            FREEWAY,
            MULTIPLE_CARRIAGEWAY,
            SINGLE_CARRIAGEWAY,
            ROUNDABOUT_CIRCLE,
            TRAFFIC_SQUARE,
            SLIP_ROAD,
            PARALLEL_ROAD,
            RAMP_ON_FREEWAY,
            RAMP,
            SERVICE_ROAD,
            CAR_PARK_ENTRANCE,
            SERVICE_ENTRANCE,
            PEDESTRIAN_ZONE,
            NA,
        )
        annotation class Type

        @Type
        @JvmSynthetic
        internal fun createFromNativeObject(nativeObj: com.mapbox.navigator.FormOfWay): Int =
            when (nativeObj) {
                com.mapbox.navigator.FormOfWay.FREEWAY -> FREEWAY
                com.mapbox.navigator.FormOfWay.MULTIPLE_CARRIAGEWAY -> MULTIPLE_CARRIAGEWAY
                com.mapbox.navigator.FormOfWay.SINGLE_CARRIAGEWAY -> SINGLE_CARRIAGEWAY
                com.mapbox.navigator.FormOfWay.ROUNDABOUT_CIRCLE -> ROUNDABOUT_CIRCLE
                com.mapbox.navigator.FormOfWay.TRAFFIC_SQUARE -> TRAFFIC_SQUARE
                com.mapbox.navigator.FormOfWay.SLIP_ROAD -> SLIP_ROAD
                com.mapbox.navigator.FormOfWay.PARALLEL_ROAD -> PARALLEL_ROAD
                com.mapbox.navigator.FormOfWay.RAMP_ON_FREEWAY -> RAMP_ON_FREEWAY
                com.mapbox.navigator.FormOfWay.RAMP -> RAMP
                com.mapbox.navigator.FormOfWay.SERVICE_ROAD -> SERVICE_ROAD
                com.mapbox.navigator.FormOfWay.CAR_PARK_ENTRANCE -> CAR_PARK_ENTRANCE
                com.mapbox.navigator.FormOfWay.SERVICE_ENTRANCE -> SERVICE_ENTRANCE
                com.mapbox.navigator.FormOfWay.PEDESTRIAN_ZONE -> PEDESTRIAN_ZONE
                com.mapbox.navigator.FormOfWay.NA -> NA
                com.mapbox.navigator.FormOfWay.UNKNOWN -> UNKNOWN
            }
    }

    /**
     * ETC2 road type.
     */
    object Etc2Road {

        /**
         * Unknown ETC2 road type.
         */
        const val UNKNOWN = 0

        /**
         * Highway ETC2 road type.
         */
        const val HIGHWAY = 1

        /**
         * City highway ETC2 road type.
         */
        const val CITY_HIGHWAY = 2

        /**
         * Normal road ETC2 road type.
         */
        const val NORMAL_ROAD = 3

        /**
         * Other ETC2 road type.
         */
        const val OTHER = 4

        /**
         * Retention policy for the [Etc2Road.Type].
         */
        @Retention(AnnotationRetention.BINARY)
        @IntDef(
            UNKNOWN,
            HIGHWAY,
            CITY_HIGHWAY,
            NORMAL_ROAD,
            OTHER,
        )
        annotation class Type

        @Type
        @JvmSynthetic
        internal fun createFromNativeObject(
            nativeObj: com.mapbox.navigator.ETC2RoadType,
        ): Int {
            return when (nativeObj) {
                com.mapbox.navigator.ETC2RoadType.HIGHWAY -> HIGHWAY
                com.mapbox.navigator.ETC2RoadType.CITY_HIGHWAY -> CITY_HIGHWAY
                com.mapbox.navigator.ETC2RoadType.NORMAL_ROAD -> NORMAL_ROAD
                com.mapbox.navigator.ETC2RoadType.OTHER -> OTHER
                com.mapbox.navigator.ETC2RoadType.UNKNOWN -> UNKNOWN
            }
        }
    }

    internal companion object {

        @JvmSynthetic
        fun createFromNativeObject(nativeObj: com.mapbox.navigator.EdgeAdasAttributes) =
            AdasEdgeAttributes(
                speedLimit = nativeObj.speedLimit.map {
                    AdasSpeedLimitInfo.createFromNativeObject(it)
                },
                slopes = nativeObj.slopes.map {
                    AdasValueOnEdge.createFromNativeObject(it)
                },
                elevations = nativeObj.elevations.map {
                    AdasValueOnEdge.createFromNativeObject(it)
                },
                curvatures = nativeObj.curvatures.map {
                    AdasValueOnEdge.createFromNativeObject(it)
                },
                isDividedRoad = nativeObj.isDividedRoad,
                formOfWay = nativeObj.formOfWay?.let { FormOfWay.createFromNativeObject(it) },
                etc2 = Etc2Road.createFromNativeObject(nativeObj.etc2),
            )
    }
}
