package com.mapbox.navigation.base.route.model

import com.mapbox.geojson.Point

/**
 * Object representing an intersection along the step.
 *
 * @property location A [Point] representing this intersection location.
 * @since 1.0
 *
 * @property bearings An integer list of bearing values available at the step intersection.
 * @return An array of bearing values (for example [0,90,180,270]) that are available at the
 * intersection. The bearings describe all available roads at the intersection.
 * @since 1.0
 *
 * @property classes A list of strings signifying the classes of the road exiting the intersection. Possible
 * values:
 *
 *  * **toll**: the road continues on a toll road
 *  * **ferry**: the road continues on a ferry
 *  * **restricted**: the road continues on with access restrictions
 *  * **motorway**: the road continues on a motorway
 *  * **tunnel**: the road continues on a tunnel
 *
 * @return a string list containing the classes of the road exiting the intersection
 * @since 1.0
 *
 * @property entry A list of entry flags, corresponding in a 1:1 relationship to the bearings. A value of true
 * indicates that the respective road could be entered on a valid route. false indicates that the
 * turn onto the respective road would violate a restriction.
 *
 * @return a list of entry flags, corresponding in a 1:1 relationship to the bearings
 * @since 1.0
 *
 * @property into Index into bearings/entry array. Used to calculate the bearing before the turn. Namely, the
 * clockwise angle from true north to the direction of travel before the maneuver/passing the
 * intersection. To get the bearing in the direction of driving, the bearing has to be rotated by
 * a value of 180. The value is not supplied for departure
 * maneuvers.
 *
 * @return index into bearings/entry array
 * @since 1.0
 *
 * @property out Index out of the bearings/entry array. Used to extract the bearing after the turn. Namely, The
 * clockwise angle from true north to the direction of travel after the maneuver/passing the
 * intersection. The value is not supplied for arrive maneuvers.
 *
 * @return index out of the bearings/entry array
 * @since 1.0
 *
 * @property lanes Array of lane objects that represent the available turn lanes at the intersection. If no lane
 * information is available for an intersection, the lanes property will not be present. Lanes are
 * provided in their order on the street, from left to right.
 *
 * @return array of lane objects that represent the available turn lanes at the intersection
 * @since 1.0
 */
class StepIntersectionNavigation(
    val location: Point,
    val bearings: List<Int>?,
    val classes: List<String>?,
    val entry: List<Boolean>?,
    val into: Int?,
    val out: Int?,
    val lanes: List<IntersectionLanesNavigation>?
) {
    val rawLocation: DoubleArray
        get() = doubleArrayOf(location.longitude(), location.latitude())
}
