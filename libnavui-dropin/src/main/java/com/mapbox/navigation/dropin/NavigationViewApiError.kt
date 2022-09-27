package com.mapbox.navigation.dropin

import androidx.annotation.IntDef
import com.mapbox.api.directions.v5.models.DirectionsWaypoint

/**
 * Error returned by the [NavigationViewApi].
 *
 * @param type The error type (see [NavigationViewApiErrorTypes]).
 * @param message The detail message string.
 */
class NavigationViewApiError internal constructor(
    @NavigationViewApiErrorType val type: Int,
    message: String
) : Throwable(message)

/**
 * All Error types returned by the NavigationApi.
 */
object NavigationViewApiErrorTypes {
    /**
     * Error returned when the destination hasn't been set yet.
     */
    const val MissingDestinationInfo = 1

    /**
     * Error returned when the preview routes list hasn't been set yet.
     */
    const val MissingPreviewRoutesInfo = 2

    /**
     * Error returned when the routes list hasn't been set yet.
     */
    const val MissingRoutesInfo = 3

    /**
     * Error returned when given preview routes or routes list is empty.
     */
    const val InvalidRoutesInfo = 4

    /**
     * Error returned when given preview routes or routes list is missing [DirectionsWaypoint]
     * information that is needed to determine destination coordinates.
     */
    const val IncompleteRoutesInfo = 5
}

/**
 * Denotes that the annotated element value should be one of the
 * [NavigationViewApiErrorTypes] constants.
 */
@Retention(AnnotationRetention.BINARY)
@IntDef(
    NavigationViewApiErrorTypes.MissingDestinationInfo,
    NavigationViewApiErrorTypes.MissingPreviewRoutesInfo,
    NavigationViewApiErrorTypes.MissingRoutesInfo,
    NavigationViewApiErrorTypes.InvalidRoutesInfo,
    NavigationViewApiErrorTypes.IncompleteRoutesInfo
)
annotation class NavigationViewApiErrorType
