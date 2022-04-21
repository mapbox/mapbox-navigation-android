package com.mapbox.navigation.ui.maps.camera.data

import android.location.Location
import com.mapbox.api.directions.v5.models.StepManeuver

/**
 * Options that impact generation of frames.
 */
class MapboxNavigationViewportDataSourceOptions internal constructor() {
    /**
     * Options that impact generation of following frames.
     */
    val followingFrameOptions = FollowingFrameOptions()

    /**
     * Options that impact generation of overview frames.
     */
    val overviewFrameOptions = OverviewFrameOptions()
}

/**
 * Options that impact the generation of the following frame.
 */
class FollowingFrameOptions internal constructor() {

    /**
     * The default pitch that will be generated for following camera frames.
     *
     * Defaults to `45.0` degrees.
     */
    var defaultPitch = 45.0

    /**
     * The min zoom that will be generated for camera following frames.
     *
     * Defaults to `10.5`.
     */
    var minZoom = 10.5

    /**
     * The max zoom that will be generated for camera following frames.
     *
     * Defaults to `16.35`.
     */
    var maxZoom = 16.35

    /**
     * When a produced **following frame** has pitch `0` and there are at least 2 points available for framing,
     * the puck will not be tied to the bottom edge of the [MapboxNavigationViewportDataSource.followingPadding] and instead move
     * around the centroid of the framed geometry (user location plus additional points to frame together or maneuver if route is available)
     * to maximize the view of that geometry within the [MapboxNavigationViewportDataSource.followingPadding].
     *
     * Defaults to `true`.
     */
    var maximizeViewableGeometryWhenPitchZero = true

    /**
     * Options that modify the framed route geometries based on the intersection density.
     *
     * By default we frame the whole remainder of the step while the options here shrink that geometry to increase the zoom level.
     */
    val intersectionDensityCalculation = IntersectionDensityCalculation()

    /**
     * Options that modify the framed route geometries when approaching a maneuver.
     */
    val pitchNearManeuvers = PitchNearManeuvers()

    /**
     * Options that modify the framed route geometries by appending additional points after maneuver to extend the view.
     */
    val frameGeometryAfterManeuver = FrameGeometryAfterManeuver()

    /**
     * Options that impact bearing generation to not be fixed to location's bearing but also taking into the direction to the upcoming maneuver.
     */
    val bearingSmoothing = BearingSmoothing()

    /**
     * If `true`, the source will manipulate Camera Center Property when producing following frame
     * updates as necessary.
     *
     * If `false`, the source will not change the current Camera Center Property.
     *
     * Defaults to `true`.
     */
    var centerUpdatesAllowed = true

    /**
     * If `true`, the source will manipulate Camera Zoom Property when producing following frame
     * updates as necessary.
     *
     * If `false`, the source will not change the current Zoom Center Property.
     *
     * Defaults to `true`.
     */
    var zoomUpdatesAllowed = true

    /**
     * If `true`, the source will manipulate Camera Bearing Property when producing following frame
     * updates as necessary.
     *
     * If `false`, the source will not change the current Camera Bearing Property.
     *
     * Defaults to `true`.
     */
    var bearingUpdatesAllowed = true

    /**
     * If `true`, the source will manipulate Camera Pitch Property when producing following frame
     * updates as necessary.
     *
     * If `false`, the source will not change the current Camera Pitch Property.
     *
     * Defaults to `true`.
     */
    var pitchUpdatesAllowed = true

    /**
     * If `true`, the source will manipulate Camera Padding Property when producing following frame
     * updates as necessary.
     *
     * If `false`, the source will not change the current Camera Padding Property.
     *
     * Defaults to `true`.
     */
    var paddingUpdatesAllowed = true

    /**
     * Options that modify the framed route geometries based on the intersection density.
     *
     * By default we frame the whole remainder of the step while the options here shrink that geometry to increase the zoom level.
     */
    class IntersectionDensityCalculation internal constructor() {
        /**
         * **Preconditions**:
         * - a route is provided via [MapboxNavigationViewportDataSource.onRouteChanged]
         * - updates are provided via [MapboxNavigationViewportDataSource.onRouteProgressChanged]
         *
         * When this option is enabled and the preconditions are met,
         * the geometry that's going to be **framed for following** will not match the whole remainder of the current step
         * but a smaller subset of that geometry to make the zoom level higher.
         *
         * This has an effect of zooming closer in urban locations when intersections are dense and zooming out on highways where opportunities to turn are farther apart.
         *
         * Defaults to `true`.
         */
        var enabled = true

        /**
         * When enabled this multiplier can be used to adjust the size of the portion of the remaining step that's going to be selected for framing.
         *
         * Defaults to `7.0`.
         */
        var averageDistanceMultiplier = 7.0

        /**
         * When enabled, this describes the minimum distance between intersections to count them as 2 instances.
         *
         * This has an effect of filtering out intersections based on parking lot entrances, driveways and alleys from the average intersection distance.
         *
         * Defaults to `20.0` meters.
         */
        var minimumDistanceBetweenIntersections = 20.0
    }

    /**
     * Options that modify the framed route geometries when approaching a maneuver.
     */
    class PitchNearManeuvers internal constructor() {
        /**
         * **Preconditions**:
         * - a route is provided via [MapboxNavigationViewportDataSource.onRouteChanged]
         * - updates are provided via [MapboxNavigationViewportDataSource.onRouteProgressChanged]
         *
         * When enabled the generated **following camera frame** will have pitch `0` when [triggerDistanceFromManeuver] is met.
         *
         * Defaults to `true`.
         */
        var enabled = true

        /**
         * When this option is enabled and the preconditions are met,
         * this variable describes the threshold distance to the next maneuver makes the frame with pitch `0`, based on the [MapboxNavigationViewportDataSource.onRouteProgressChanged].
         *
         * Defaults to `180.0` meters.
         */
        var triggerDistanceFromManeuver = 180.0

        /**
         * List of maneuvers for which camera frames pitch should not be set to `0`.
         *
         * Defaults to `listOf("continue", "merge", "on ramp", "off ramp", "fork")`.
         *
         * See [available maneuver types](https://docs.mapbox.com/api/navigation/directions/#maneuver-types) and [StepManeuver] class for more options.
         */
        var excludedManeuvers: List<String> = listOf(
            StepManeuver.CONTINUE,
            StepManeuver.MERGE,
            StepManeuver.ON_RAMP,
            StepManeuver.OFF_RAMP,
            StepManeuver.FORK
        )
    }

    /**
     * Options that modify the framed route geometries by appending additional points after maneuver to extend the view.
     */
    class FrameGeometryAfterManeuver internal constructor() {
        /**
         * **Preconditions**:
         * - a route is provided via [MapboxNavigationViewportDataSource.onRouteChanged]
         * - updates are provided via [MapboxNavigationViewportDataSource.onRouteProgressChanged]
         * - produced **following frame** has pitch `0`
         *
         * When this option is enabled and the preconditions are met,
         * this controls whether additional points _after_ the upcoming maneuver should be framed to provide more context.
         *
         * Defaults to `true`.
         */
        var enabled = true

        /**
         * When enabled, this controls the distance between maneuvers closely following the current one to treat them for inclusion in the frame.
         *
         * Defaults to `150.0` meters.
         */
        var distanceToCoalesceCompoundManeuvers = 150.0

        /**
         * When enabled, this controls the distance on route after the current maneuver to include in the frame.
         *
         * This is added on top of potentially included compound maneuvers that closely follow the upcoming one,
         * controlled by [distanceToCoalesceCompoundManeuvers].
         *
         * Defaults to `100.0` meters.
         */
        var distanceToFrameAfterManeuver = 100.0
    }

    /**
     * Options that impact bearing generation to not be fixed to location's bearing but also taking into the direction to the upcoming maneuver.
     */
    class BearingSmoothing internal constructor() {
        /**
         * If enabled, the **following frame**'s bearing won't exactly reflect the bearing returned by the [Location] from [MapboxNavigationViewportDataSource.onLocationChanged]
         * but will also be affected by the direction to the upcoming framed geometry, to maximize the viewable area.
         *
         * Defaults to `true`.
         *
         * @see [maxBearingAngleDiff]
         */
        var enabled = true

        /**
         * When enabled, this controls how much the **following frame**'s bearing can deviate from the [Location] bearing, in degrees.
         *
         * Defaults to `45.0` degrees.
         */
        var maxBearingAngleDiff = 45.0
    }
}

/**
 * Options that impact the generation of the overview frame.
 */
class OverviewFrameOptions internal constructor() {

    /**
     * The max zoom that will be generated for camera overview frames.
     *
     * Defaults to `16.35`.
     */
    var maxZoom = 16.35

    /**
     * Options that impact the simplification of geometries framed for overview.
     *
     * Simplifying geometries, especially for longer routes, can have a significant impact on the performance of generating frames and each [MapboxNavigationViewportDataSource.evaluate] calls.
     */
    val geometrySimplification = GeometrySimplification()

    /**
     * If `true`, the source will manipulate Camera Center Property when producing overview frame
     * updates as necessary.
     *
     * If `false`, the source will not change the current Camera Center Property.
     *
     * Defaults to `true`.
     */
    var centerUpdatesAllowed = true

    /**
     * If `true`, the source will manipulate Camera Zoom Property when producing overview frame
     * updates as necessary.
     *
     * If `false`, the source will not change the current Zoom Center Property.
     *
     * Defaults to `true`.
     */
    var zoomUpdatesAllowed = true

    /**
     * If `true`, the source will manipulate Camera Bearing Property when producing overview frame
     * updates as necessary.
     *
     * If `false`, the source will not change the current Camera Bearing Property.
     *
     * Defaults to `true`.
     */
    var bearingUpdatesAllowed = true

    /**
     * If `true`, the source will manipulate Camera Pitch Property when producing overview frame
     * updates as necessary.
     *
     * If `false`, the source will not change the current Camera Pitch Property.
     *
     * Defaults to `true`.
     */
    var pitchUpdatesAllowed = true

    /**
     * If `true`, the source will manipulate Camera Padding Property when producing overview frame
     * updates as necessary.
     *
     * If `false`, the source will not change the current Camera Padding Property.
     *
     * Defaults to `true`.
     */
    var paddingUpdatesAllowed = true

    /**
     * Options that impact the simplification of geometries framed for overview.
     *
     * Simplifying geometries, especially for longer routes, can have a significant impact on the performance of generating frames and each [MapboxNavigationViewportDataSource.evaluate] calls.
     */
    class GeometrySimplification internal constructor() {

        /**
         * **Preconditions**:
         * - a route is provided via [MapboxNavigationViewportDataSource.onRouteChanged]
         *
         * If `true`, the frames generated for overview will use a simplified route geometry.
         *
         * Defaults to `true`.
         *
         * @see simplificationFactor
         */
        var enabled = true

        /**
         * Determines how many points of each step's geometry should be removed from framing.
         *
         * The default value of `25` means that every 25th point in the geometry will be taken for framing, plus the first and last point of each step.
         *
         * The factor has to be a positive integer.
         */
        var simplificationFactor = 25
    }
}
