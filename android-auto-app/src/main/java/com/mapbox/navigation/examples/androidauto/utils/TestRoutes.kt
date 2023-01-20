package com.mapbox.navigation.examples.androidauto.utils

import com.mapbox.geojson.Point

/**
 * Coordinates containing `subType = JCT`
 * 139.7745686, 35.677573;139.784915, 35.680960
 * https://api.mapbox.com/guidance-views/v1/709948800/jct/CA075101?arrow_ids=CA07510E
 *
 * Coordinates containing `subType` = SAPA`
 * 137.76136788022933, 34.83891088143494;137.75220947550804, 34.840924660770725
 * https://api.mapbox.com/guidance-views/v1/709948800/sapa/SA117201?arrow_ids=SA11720A
 *
 * Coordinates containing `subType` = CITYREAL`
 * 139.68153626083233, 35.66812853462302;139.68850488593154, 35.66099697148769
 * https://api.mapbox.com/guidance-views/v1/709948800/cityreal/13c00282_o40d?arrow_ids=13c00282_o41a
 *
 * Coordinates containing `subType` = TOLLBRANCH`
 * 137.02725, 35.468588;137.156787, 35.372602
 * https://api.mapbox.com/guidance-views/v1/709948800/tollbranch/CR896101?arrow_ids=CR89610A
 *
 * Coordinates containing `subType` = AFTERTOLL`
 * 141.4223967090212, 43.07693368987961;141.42118630948409, 43.07604662044662
 * https://api.mapbox.com/guidance-views/v1/709948800/aftertoll/HW00101805?arrow_ids=HW00101805_1
 *
 * Coordinates containing `subType` = EXPRESSWAY_ENTRANCE`
 * 139.724088, 35.672885; 139.630359, 35.626416
 * https://api.mapbox.com/guidance-views/v1/709948800/entrance/13i00015_o10d?arrow_ids=13i00015_o11a
 *
 * Coordinates containing `subType` = EXPRESSWAY_EXIT`
 * 135.324023, 34.715952;135.296332, 34.711387
 * https://api.mapbox.com/guidance-views/v1/709948800/exit/28o00022_o20d?arrow_ids=28o00022_o21a
 */
internal enum class TestRoutes(
    val origin: Point,
    val destination: Point
) {
    JCT(
        Point.fromLngLat(139.7745686, 35.677573),
        Point.fromLngLat(139.784915, 35.680960)
    ),
    SAPA(
        Point.fromLngLat(137.76136788022933, 34.83891088143494),
        Point.fromLngLat(137.75220947550804, 34.840924660770725)
    ),
    CITYREAL(
        Point.fromLngLat(139.68153626083233, 35.66812853462302),
        Point.fromLngLat(139.68850488593154, 35.66099697148769)
    ),
    TOLLBRANCH(
        Point.fromLngLat(137.02725, 35.468588),
        Point.fromLngLat(137.156787, 35.372602)
    ),
    AFTERTOLL(
        Point.fromLngLat(141.4223967090212, 43.07693368987961),
        Point.fromLngLat(141.42118630948409, 43.07604662044662)
    ),
    EXPRESSWAY_ENTRANCE(
        Point.fromLngLat(139.724088, 35.672885),
        Point.fromLngLat(139.630359, 35.626416)
    ),
    EXPRESSWAY_EXIT(
        Point.fromLngLat(135.324023, 34.715952),
        Point.fromLngLat(135.296332, 34.711387)
    );

    operator fun component1(): Point = origin
    operator fun component2(): Point = destination
}
