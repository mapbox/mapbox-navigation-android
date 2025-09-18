package com.mapbox.navigation.core.adas

import androidx.annotation.IntDef
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Road item types for ADAS (Advanced Driver Assistance Systems).
 *
 * These constants represent various road signs, traffic control devices,
 * and road infrastructure elements that can be detected and reported
 * by the navigation system.
 */
@ExperimentalPreviewMapboxNavigationAPI
object AdasRoadItemType {

    /** General danger warning sign. */
    const val DANGER_SIGN = 0

    /** Pass left or right side sign. */
    const val PASS_LEFT_OR_RIGHT_SIDE_SIGN = 1

    /** Pass left side only sign. */
    const val PASS_LEFT_SIDE_SIGN = 2

    /** Pass right side only sign. */
    const val PASS_RIGHT_SIDE_SIGN = 3

    /** Domestic animals crossing warning sign. */
    const val DOMESTIC_ANIMALS_CROSSING_SIGN = 4

    /** Wild animals crossing warning sign. */
    const val WILD_ANIMALS_CROSSING_SIGN = 5

    /** Road works or construction warning sign. */
    const val ROAD_WORKS_SIGN = 6

    /** Residential area sign indicating beginning of residential zone. */
    const val RESIDENTIAL_AREA_SIGN = 7

    /** End of residential area sign. */
    const val END_OF_RESIDENTIAL_AREA_SIGN = 8

    /** Right bend warning sign. */
    const val RIGHT_BEND_SIGN = 9

    /** Left bend warning sign. */
    const val LEFT_BEND_SIGN = 10

    /** Double bend warning sign with right bend first. */
    const val DOUBLE_BEND_RIGHT_FIRST_SIGN = 11

    /** Double bend warning sign with left bend first. */
    const val DOUBLE_BEND_LEFT_FIRST_SIGN = 12

    /** Curvy road warning sign indicating multiple curves ahead. */
    const val CURVY_ROAD_SIGN = 13

    /** Overtaking by goods vehicles prohibited sign. */
    const val OVERTAKING_BY_GOODS_VEHICLES_PROHIBITED_SIGN = 14

    /** End of prohibition on overtaking for goods vehicles sign. */
    const val END_OF_PROHIBITION_ON_OVERTAKING_FOR_GOODS_VEHICLES_SIGN = 15

    /** Dangerous intersection warning sign. */
    const val DANGEROUS_INTERSECTION_SIGN = 16

    /** Tunnel warning sign. */
    const val TUNNEL_SIGN = 17

    /** Ferry terminal sign. */
    const val FERRY_TERMINAL_SIGN = 18

    /** Narrow bridge warning sign. */
    const val NARROW_BRIDGE_SIGN = 19

    /** Humpback bridge warning sign. */
    const val HUMPBACK_BRIDGE_BRIDGE_SIGN = 20

    /** River bank warning sign. */
    const val RIVER_BANK_SIGN = 21

    /** River bank on left side warning sign. */
    const val RIVER_BANK_LEFT_SIGN = 22

    /** Yield sign. */
    const val YIELD_SIGN = 23

    /** Stop sign. */
    const val STOP_SIGN = 24

    /** Priority road sign. */
    const val PRIORITY_ROAD_SIGN = 25

    /** General intersection warning sign. */
    const val INTERSECTION_SIGN = 26

    /** Intersection with minor road warning sign. */
    const val INTERSECTION_WITH_MINOR_ROAD_SIGN = 27

    /** Intersection with priority to the right warning sign. */
    const val INTERSECTION_WITH_PRIORITY_TO_THE_RIGHT_SIGN = 28

    /** Direction arrow pointing to the right. */
    const val DIRECTION_TO_THE_RIGHT_SIGN = 29

    /** Direction arrow pointing to the left. */
    const val DIRECTION_TO_THE_LEFT_SIGN = 30

    /** Carriageway narrows warning sign. */
    const val CARRIAGEWAY_NARROWS_SIGN = 31

    /** Carriageway narrows on the right side warning sign. */
    const val CARRIAGEWAY_NARROWS_RIGHT_SIGN = 32

    /** Carriageway narrows on the left side warning sign. */
    const val CARRIAGEWAY_NARROWS_LEFT_SIGN = 33

    /** Lane merge from left warning sign. */
    const val LANE_MERGE_LEFT_SIGN = 34

    /** Lane merge from right warning sign. */
    const val LANE_MERGE_RIGHT_SIGN = 35

    /** Lane merge from center warning sign. */
    const val LANE_MERGE_CENTER_SIGN = 36

    /** Overtaking prohibited sign. */
    const val OVERTAKING_PROHIBITED_SIGN = 37

    /** End of prohibition on overtaking sign. */
    const val END_OF_PROHIBITION_ON_OVERTAKING_SIGN = 38

    /** Protective overtaking sign. */
    const val PROTECTIVE_OVERTAKING_SIGN = 39

    /** Pedestrians crossing warning sign. */
    const val PEDESTRIANS_SIGN = 40

    /** Pedestrian crossing sign. */
    const val PEDESTRIAN_CROSSING_SIGN = 41

    /** Children warning sign. */
    const val CHILDREN_SIGN = 42

    /** School zone warning sign. */
    const val SCHOOL_ZONE_SIGN = 43

    /** Cyclists warning sign. */
    const val CYCLISTS_SIGN = 44

    /** Two-way traffic warning sign. */
    const val TWO_WAY_TRAFFIC_SIGN = 45

    /** Railway crossing with gates warning sign. */
    const val RAILWAY_CROSSING_WITH_GATES_SIGN = 46

    /** Railway crossing without gates warning sign. */
    const val RAILWAY_CROSSING_WITHOUT_GATES_SIGN = 47

    /** General railway crossing warning sign. */
    const val RAILWAY_CROSSING_SIGN = 48

    /** Tramway crossing warning sign. */
    const val TRAMWAY_SIGN = 49

    /** Falling rocks warning sign. */
    const val FALLING_ROCKS_SIGN = 50

    /** Falling rocks from left side warning sign. */
    const val FALLING_ROCKS_LEFT_SIGN = 51

    /** Falling rocks from right side warning sign. */
    const val FALLING_ROCKS_RIGHT_SIGN = 52

    /** Steep drop on left side warning sign. */
    const val STEEP_DROP_LEFT_SIGN = 53

    /** Steep drop on right side warning sign. */
    const val STEEP_DROP_RIGHT_SIGN = 54

    /** Variable sign with mechanical elements. */
    const val VARIABLE_SIGN_MECHANIC_ELEMENTS_SIGN = 55

    /** Slippery road warning sign. */
    const val SLIPPERY_ROAD_SIGN = 56

    /** Steep ascent warning sign. */
    const val STEEP_ASCENT_SIGN = 57

    /** Steep descent warning sign. */
    const val STEEP_DESCENT_SIGN = 58

    /** Uneven road surface warning sign. */
    const val UNEVEN_ROAD_SIGN = 59

    /** Road hump or speed bump warning sign. */
    const val HUMP_SIGN = 60

    /** Road dip warning sign. */
    const val DIP_SIGN = 61

    /** Road floods or water hazard warning sign. */
    const val ROAD_FLOODS_SIGN = 62

    /** Icy road conditions warning sign. */
    const val ICY_ROAD_SIGN = 63

    /** Side winds warning sign. */
    const val SIDE_WINDS_SIGN = 64

    /** Traffic congestion warning sign. */
    const val TRAFFIC_CONGESTION_SIGN = 65

    /** High accident area warning sign. */
    const val HIGH_ACCIDENT_AREA_SIGN = 66

    /** Variable sign with light elements. */
    const val VARIABLE_SIGN_LIGHT_ELEMENTS_SIGN = 67

    /** Priority over oncoming traffic sign. */
    const val PRIORITY_OVER_ONCOMING_TRAFFIC_SIGN = 68

    /** Priority for oncoming traffic sign. */
    const val PRIORITY_FOR_ONCOMING_TRAFFIC_SIGN = 69

    /** Speed limit sign. */
    const val SPEED_LIMIT_SIGN = 70

    /** Toll booth location. */
    const val TOLL_BOOTH = 71

    /** Road camera marking end of speed monitoring interval. */
    const val ROAD_CAM_SPEED_INTERVAL_END = 72

    /** Road camera marking start of speed monitoring interval. */
    const val ROAD_CAM_SPEED_INTERVAL_START = 73

    /** Road camera for speed monitoring over an interval. */
    const val ROAD_CAM_SPEED_INTERVAL = 74

    /** Road camera monitoring non-motorized vehicle lane. */
    const val ROAD_CAM_LANE_NON_MOTORIZED = 75

    /** Road camera monitoring emergency lane usage. */
    const val ROAD_CAM_LANE_EMERGENCY = 76

    /** Road camera monitoring bus lane usage. */
    const val ROAD_CAM_LANE_BUS = 77

    /** Road camera for general traffic violation monitoring. */
    const val ROAD_CAM_VIOLATION = 78

    /** Road camera for red light violation monitoring. */
    const val ROAD_CAM_RED_LIGHT = 79

    /** Road camera for general surveillance. */
    const val ROAD_CAM_SURVEILLANCE = 80

    /** Road camera displaying current speed to drivers. */
    const val ROAD_CAM_SPEED_CURRENT_SPEED = 81

    /** Railroad crossing location. */
    const val RAILROAD_CROSSING = 82

    /** Zebra crossing (pedestrian crosswalk). */
    const val ZEBRA = 83

    /** Speed bump. */
    const val SPEED_BUMP = 84

    /** Traffic light. */
    const val TRAFFIC_LIGHT = 85

    /**
     * Retention policy for the [AdasRoadItemType.Type].
     */
    @Retention(AnnotationRetention.BINARY)
    @IntDef(
        DANGER_SIGN,
        PASS_LEFT_OR_RIGHT_SIDE_SIGN,
        PASS_LEFT_SIDE_SIGN,
        PASS_RIGHT_SIDE_SIGN,
        DOMESTIC_ANIMALS_CROSSING_SIGN,
        WILD_ANIMALS_CROSSING_SIGN,
        ROAD_WORKS_SIGN,
        RESIDENTIAL_AREA_SIGN,
        END_OF_RESIDENTIAL_AREA_SIGN,
        RIGHT_BEND_SIGN,
        LEFT_BEND_SIGN,
        DOUBLE_BEND_RIGHT_FIRST_SIGN,
        DOUBLE_BEND_LEFT_FIRST_SIGN,
        CURVY_ROAD_SIGN,
        OVERTAKING_BY_GOODS_VEHICLES_PROHIBITED_SIGN,
        END_OF_PROHIBITION_ON_OVERTAKING_FOR_GOODS_VEHICLES_SIGN,
        DANGEROUS_INTERSECTION_SIGN,
        TUNNEL_SIGN,
        FERRY_TERMINAL_SIGN,
        NARROW_BRIDGE_SIGN,
        HUMPBACK_BRIDGE_BRIDGE_SIGN,
        RIVER_BANK_SIGN,
        RIVER_BANK_LEFT_SIGN,
        YIELD_SIGN,
        STOP_SIGN,
        PRIORITY_ROAD_SIGN,
        INTERSECTION_SIGN,
        INTERSECTION_WITH_MINOR_ROAD_SIGN,
        INTERSECTION_WITH_PRIORITY_TO_THE_RIGHT_SIGN,
        DIRECTION_TO_THE_RIGHT_SIGN,
        DIRECTION_TO_THE_LEFT_SIGN,
        CARRIAGEWAY_NARROWS_SIGN,
        CARRIAGEWAY_NARROWS_RIGHT_SIGN,
        CARRIAGEWAY_NARROWS_LEFT_SIGN,
        LANE_MERGE_LEFT_SIGN,
        LANE_MERGE_RIGHT_SIGN,
        LANE_MERGE_CENTER_SIGN,
        OVERTAKING_PROHIBITED_SIGN,
        END_OF_PROHIBITION_ON_OVERTAKING_SIGN,
        PROTECTIVE_OVERTAKING_SIGN,
        PEDESTRIANS_SIGN,
        PEDESTRIAN_CROSSING_SIGN,
        CHILDREN_SIGN,
        SCHOOL_ZONE_SIGN,
        CYCLISTS_SIGN,
        TWO_WAY_TRAFFIC_SIGN,
        RAILWAY_CROSSING_WITH_GATES_SIGN,
        RAILWAY_CROSSING_WITHOUT_GATES_SIGN,
        RAILWAY_CROSSING_SIGN,
        TRAMWAY_SIGN,
        FALLING_ROCKS_SIGN,
        FALLING_ROCKS_LEFT_SIGN,
        FALLING_ROCKS_RIGHT_SIGN,
        STEEP_DROP_LEFT_SIGN,
        STEEP_DROP_RIGHT_SIGN,
        VARIABLE_SIGN_MECHANIC_ELEMENTS_SIGN,
        SLIPPERY_ROAD_SIGN,
        STEEP_ASCENT_SIGN,
        STEEP_DESCENT_SIGN,
        UNEVEN_ROAD_SIGN,
        HUMP_SIGN,
        DIP_SIGN,
        ROAD_FLOODS_SIGN,
        ICY_ROAD_SIGN,
        SIDE_WINDS_SIGN,
        TRAFFIC_CONGESTION_SIGN,
        HIGH_ACCIDENT_AREA_SIGN,
        VARIABLE_SIGN_LIGHT_ELEMENTS_SIGN,
        PRIORITY_OVER_ONCOMING_TRAFFIC_SIGN,
        PRIORITY_FOR_ONCOMING_TRAFFIC_SIGN,
        SPEED_LIMIT_SIGN,
        TOLL_BOOTH,
        ROAD_CAM_SPEED_INTERVAL_END,
        ROAD_CAM_SPEED_INTERVAL_START,
        ROAD_CAM_SPEED_INTERVAL,
        ROAD_CAM_LANE_NON_MOTORIZED,
        ROAD_CAM_LANE_EMERGENCY,
        ROAD_CAM_LANE_BUS,
        ROAD_CAM_VIOLATION,
        ROAD_CAM_RED_LIGHT,
        ROAD_CAM_SURVEILLANCE,
        ROAD_CAM_SPEED_CURRENT_SPEED,
        RAILROAD_CROSSING,
        ZEBRA,
        SPEED_BUMP,
        TRAFFIC_LIGHT,
    )
    annotation class Type

    @JvmSynthetic
    @Type
    internal fun createFromNativeObject(nativeObj: com.mapbox.navigator.RoadItemType): Int =
        when (nativeObj) {
            /* ktlint-disable max-line-length */
            com.mapbox.navigator.RoadItemType.DANGER_SIGN -> DANGER_SIGN
            com.mapbox.navigator.RoadItemType.PASS_LEFT_OR_RIGHT_SIDE_SIGN -> PASS_LEFT_OR_RIGHT_SIDE_SIGN
            com.mapbox.navigator.RoadItemType.PASS_LEFT_SIDE_SIGN -> PASS_LEFT_SIDE_SIGN
            com.mapbox.navigator.RoadItemType.PASS_RIGHT_SIDE_SIGN -> PASS_RIGHT_SIDE_SIGN
            com.mapbox.navigator.RoadItemType.DOMESTIC_ANIMALS_CROSSING_SIGN -> DOMESTIC_ANIMALS_CROSSING_SIGN
            com.mapbox.navigator.RoadItemType.WILD_ANIMALS_CROSSING_SIGN -> WILD_ANIMALS_CROSSING_SIGN
            com.mapbox.navigator.RoadItemType.ROAD_WORKS_SIGN -> ROAD_WORKS_SIGN
            com.mapbox.navigator.RoadItemType.RESIDENTIAL_AREA_SIGN -> RESIDENTIAL_AREA_SIGN
            com.mapbox.navigator.RoadItemType.END_OF_RESIDENTIAL_AREA_SIGN -> END_OF_RESIDENTIAL_AREA_SIGN
            com.mapbox.navigator.RoadItemType.RIGHT_BEND_SIGN -> RIGHT_BEND_SIGN
            com.mapbox.navigator.RoadItemType.LEFT_BEND_SIGN -> LEFT_BEND_SIGN
            com.mapbox.navigator.RoadItemType.DOUBLE_BEND_RIGHT_FIRST_SIGN -> DOUBLE_BEND_RIGHT_FIRST_SIGN
            com.mapbox.navigator.RoadItemType.DOUBLE_BEND_LEFT_FIRST_SIGN -> DOUBLE_BEND_LEFT_FIRST_SIGN
            com.mapbox.navigator.RoadItemType.CURVY_ROAD_SIGN -> CURVY_ROAD_SIGN
            com.mapbox.navigator.RoadItemType.OVERTAKING_BY_GOODS_VEHICLES_PROHIBITED_SIGN -> OVERTAKING_BY_GOODS_VEHICLES_PROHIBITED_SIGN
            com.mapbox.navigator.RoadItemType.END_OF_PROHIBITION_ON_OVERTAKING_FOR_GOODS_VEHICLES_SIGN -> END_OF_PROHIBITION_ON_OVERTAKING_FOR_GOODS_VEHICLES_SIGN
            com.mapbox.navigator.RoadItemType.DANGEROUS_INTERSECTION_SIGN -> DANGEROUS_INTERSECTION_SIGN
            com.mapbox.navigator.RoadItemType.TUNNEL_SIGN -> TUNNEL_SIGN
            com.mapbox.navigator.RoadItemType.FERRY_TERMINAL_SIGN -> FERRY_TERMINAL_SIGN
            com.mapbox.navigator.RoadItemType.NARROW_BRIDGE_SIGN -> NARROW_BRIDGE_SIGN
            com.mapbox.navigator.RoadItemType.HUMPBACK_BRIDGE_BRIDGE_SIGN -> HUMPBACK_BRIDGE_BRIDGE_SIGN
            com.mapbox.navigator.RoadItemType.RIVER_BANK_SIGN -> RIVER_BANK_SIGN
            com.mapbox.navigator.RoadItemType.RIVER_BANK_LEFT_SIGN -> RIVER_BANK_LEFT_SIGN
            com.mapbox.navigator.RoadItemType.YIELD_SIGN -> YIELD_SIGN
            com.mapbox.navigator.RoadItemType.STOP_SIGN -> STOP_SIGN
            com.mapbox.navigator.RoadItemType.PRIORITY_ROAD_SIGN -> PRIORITY_ROAD_SIGN
            com.mapbox.navigator.RoadItemType.INTERSECTION_SIGN -> INTERSECTION_SIGN
            com.mapbox.navigator.RoadItemType.INTERSECTION_WITH_MINOR_ROAD_SIGN -> INTERSECTION_WITH_MINOR_ROAD_SIGN
            com.mapbox.navigator.RoadItemType.INTERSECTION_WITH_PRIORITY_TO_THE_RIGHT_SIGN -> INTERSECTION_WITH_PRIORITY_TO_THE_RIGHT_SIGN
            com.mapbox.navigator.RoadItemType.DIRECTION_TO_THE_RIGHT_SIGN -> DIRECTION_TO_THE_RIGHT_SIGN
            com.mapbox.navigator.RoadItemType.DIRECTION_TO_THE_LEFT_SIGN -> DIRECTION_TO_THE_LEFT_SIGN
            com.mapbox.navigator.RoadItemType.CARRIAGEWAY_NARROWS_SIGN -> CARRIAGEWAY_NARROWS_SIGN
            com.mapbox.navigator.RoadItemType.CARRIAGEWAY_NARROWS_RIGHT_SIGN -> CARRIAGEWAY_NARROWS_RIGHT_SIGN
            com.mapbox.navigator.RoadItemType.CARRIAGEWAY_NARROWS_LEFT_SIGN -> CARRIAGEWAY_NARROWS_LEFT_SIGN
            com.mapbox.navigator.RoadItemType.LANE_MERGE_LEFT_SIGN -> LANE_MERGE_LEFT_SIGN
            com.mapbox.navigator.RoadItemType.LANE_MERGE_RIGHT_SIGN -> LANE_MERGE_RIGHT_SIGN
            com.mapbox.navigator.RoadItemType.LANE_MERGE_CENTER_SIGN -> LANE_MERGE_CENTER_SIGN
            com.mapbox.navigator.RoadItemType.OVERTAKING_PROHIBITED_SIGN -> OVERTAKING_PROHIBITED_SIGN
            com.mapbox.navigator.RoadItemType.END_OF_PROHIBITION_ON_OVERTAKING_SIGN -> END_OF_PROHIBITION_ON_OVERTAKING_SIGN
            com.mapbox.navigator.RoadItemType.PROTECTIVE_OVERTAKING_SIGN -> PROTECTIVE_OVERTAKING_SIGN
            com.mapbox.navigator.RoadItemType.PEDESTRIANS_SIGN -> PEDESTRIANS_SIGN
            com.mapbox.navigator.RoadItemType.PEDESTRIAN_CROSSING_SIGN -> PEDESTRIAN_CROSSING_SIGN
            com.mapbox.navigator.RoadItemType.CHILDREN_SIGN -> CHILDREN_SIGN
            com.mapbox.navigator.RoadItemType.SCHOOL_ZONE_SIGN -> SCHOOL_ZONE_SIGN
            com.mapbox.navigator.RoadItemType.CYCLISTS_SIGN -> CYCLISTS_SIGN
            com.mapbox.navigator.RoadItemType.TWO_WAY_TRAFFIC_SIGN -> TWO_WAY_TRAFFIC_SIGN
            com.mapbox.navigator.RoadItemType.RAILWAY_CROSSING_WITH_GATES_SIGN -> RAILWAY_CROSSING_WITH_GATES_SIGN
            com.mapbox.navigator.RoadItemType.RAILWAY_CROSSING_WITHOUT_GATES_SIGN -> RAILWAY_CROSSING_WITHOUT_GATES_SIGN
            com.mapbox.navigator.RoadItemType.RAILWAY_CROSSING_SIGN -> RAILWAY_CROSSING_SIGN
            com.mapbox.navigator.RoadItemType.TRAMWAY_SIGN -> TRAMWAY_SIGN
            com.mapbox.navigator.RoadItemType.FALLING_ROCKS_SIGN -> FALLING_ROCKS_SIGN
            com.mapbox.navigator.RoadItemType.FALLING_ROCKS_LEFT_SIGN -> FALLING_ROCKS_LEFT_SIGN
            com.mapbox.navigator.RoadItemType.FALLING_ROCKS_RIGHT_SIGN -> FALLING_ROCKS_RIGHT_SIGN
            com.mapbox.navigator.RoadItemType.STEEP_DROP_LEFT_SIGN -> STEEP_DROP_LEFT_SIGN
            com.mapbox.navigator.RoadItemType.STEEP_DROP_RIGHT_SIGN -> STEEP_DROP_RIGHT_SIGN
            com.mapbox.navigator.RoadItemType.VARIABLE_SIGN_MECHANIC_ELEMENTS_SIGN -> VARIABLE_SIGN_MECHANIC_ELEMENTS_SIGN
            com.mapbox.navigator.RoadItemType.SLIPPERY_ROAD_SIGN -> SLIPPERY_ROAD_SIGN
            com.mapbox.navigator.RoadItemType.STEEP_ASCENT_SIGN -> STEEP_ASCENT_SIGN
            com.mapbox.navigator.RoadItemType.STEEP_DESCENT_SIGN -> STEEP_DESCENT_SIGN
            com.mapbox.navigator.RoadItemType.UNEVEN_ROAD_SIGN -> UNEVEN_ROAD_SIGN
            com.mapbox.navigator.RoadItemType.HUMP_SIGN -> HUMP_SIGN
            com.mapbox.navigator.RoadItemType.DIP_SIGN -> DIP_SIGN
            com.mapbox.navigator.RoadItemType.ROAD_FLOODS_SIGN -> ROAD_FLOODS_SIGN
            com.mapbox.navigator.RoadItemType.ICY_ROAD_SIGN -> ICY_ROAD_SIGN
            com.mapbox.navigator.RoadItemType.SIDE_WINDS_SIGN -> SIDE_WINDS_SIGN
            com.mapbox.navigator.RoadItemType.TRAFFIC_CONGESTION_SIGN -> TRAFFIC_CONGESTION_SIGN
            com.mapbox.navigator.RoadItemType.HIGH_ACCIDENT_AREA_SIGN -> HIGH_ACCIDENT_AREA_SIGN
            com.mapbox.navigator.RoadItemType.VARIABLE_SIGN_LIGHT_ELEMENTS_SIGN -> VARIABLE_SIGN_LIGHT_ELEMENTS_SIGN
            com.mapbox.navigator.RoadItemType.PRIORITY_OVER_ONCOMING_TRAFFIC_SIGN -> PRIORITY_OVER_ONCOMING_TRAFFIC_SIGN
            com.mapbox.navigator.RoadItemType.PRIORITY_FOR_ONCOMING_TRAFFIC_SIGN -> PRIORITY_FOR_ONCOMING_TRAFFIC_SIGN
            com.mapbox.navigator.RoadItemType.SPEED_LIMIT_SIGN -> SPEED_LIMIT_SIGN
            com.mapbox.navigator.RoadItemType.TOLL_BOOTH -> TOLL_BOOTH
            com.mapbox.navigator.RoadItemType.ROAD_CAM_SPEED_INTERVAL_END -> ROAD_CAM_SPEED_INTERVAL_END
            com.mapbox.navigator.RoadItemType.ROAD_CAM_SPEED_INTERVAL_START -> ROAD_CAM_SPEED_INTERVAL_START
            com.mapbox.navigator.RoadItemType.ROAD_CAM_SPEED_INTERVAL -> ROAD_CAM_SPEED_INTERVAL
            com.mapbox.navigator.RoadItemType.ROAD_CAM_LANE_NON_MOTORIZED -> ROAD_CAM_LANE_NON_MOTORIZED
            com.mapbox.navigator.RoadItemType.ROAD_CAM_LANE_EMERGENCY -> ROAD_CAM_LANE_EMERGENCY
            com.mapbox.navigator.RoadItemType.ROAD_CAM_LANE_BUS -> ROAD_CAM_LANE_BUS
            com.mapbox.navigator.RoadItemType.ROAD_CAM_VIOLATION -> ROAD_CAM_VIOLATION
            com.mapbox.navigator.RoadItemType.ROAD_CAM_RED_LIGHT -> ROAD_CAM_RED_LIGHT
            com.mapbox.navigator.RoadItemType.ROAD_CAM_SURVEILLANCE -> ROAD_CAM_SURVEILLANCE
            com.mapbox.navigator.RoadItemType.ROAD_CAM_SPEED_CURRENT_SPEED -> ROAD_CAM_SPEED_CURRENT_SPEED
            com.mapbox.navigator.RoadItemType.RAILROAD_CROSSING -> RAILROAD_CROSSING
            com.mapbox.navigator.RoadItemType.ZEBRA -> ZEBRA
            com.mapbox.navigator.RoadItemType.SPEED_BUMP -> SPEED_BUMP
            com.mapbox.navigator.RoadItemType.TRAFFIC_LIGHT -> TRAFFIC_LIGHT
            /* ktlint-enable max-line-length */
        }
}
