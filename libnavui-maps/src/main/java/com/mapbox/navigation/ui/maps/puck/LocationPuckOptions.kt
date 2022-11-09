package com.mapbox.navigation.ui.maps.puck

import android.content.Context
import androidx.core.content.ContextCompat
import com.mapbox.maps.plugin.LocationPuck
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.LocationPuck3D
import com.mapbox.navigation.ui.maps.R

/**
 * Gives options to specify either [LocationPuck2D] or [LocationPuck3D] references to the location
 * puck to be displayed on top of map view in each of different navigation states.
 *
 * @param context from which to reference puck drawables
 * @param freeDrivePuck stores the puck appearance to be displayed in free drive state
 * @param destinationPreviewPuck stores the puck appearance to be displayed in destination preview state
 * @param routePreviewPuck stores the puck appearance to be displayed in route preview state
 * @param activeNavigationPuck stores the puck appearance to be displayed in active navigation state
 * @param arrivalPuck stores the puck appearance to be displayed in arrival state
 */
class LocationPuckOptions private constructor(
    val context: Context,
    val freeDrivePuck: LocationPuck,
    val destinationPreviewPuck: LocationPuck,
    val routePreviewPuck: LocationPuck,
    val activeNavigationPuck: LocationPuck,
    val arrivalPuck: LocationPuck,
) {

    /**
     * @return the [Builder] that created the [LocationPuckOptions]
     */
    fun toBuilder(): Builder = Builder(context).apply {
        freeDrivePuck(freeDrivePuck)
        destinationPreviewPuck(destinationPreviewPuck)
        routePreviewPuck(routePreviewPuck)
        activeNavigationPuck(activeNavigationPuck)
        arrivalPuck(arrivalPuck)
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LocationPuckOptions

        if (context != other.context) return false
        if (freeDrivePuck != other.freeDrivePuck) return false
        if (destinationPreviewPuck != other.destinationPreviewPuck) return false
        if (routePreviewPuck != other.routePreviewPuck) return false
        if (activeNavigationPuck != other.activeNavigationPuck) return false
        if (arrivalPuck != other.arrivalPuck) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = context.hashCode()
        result = 31 * result + freeDrivePuck.hashCode()
        result = 31 * result + destinationPreviewPuck.hashCode()
        result = 31 * result + routePreviewPuck.hashCode()
        result = 31 * result + activeNavigationPuck.hashCode()
        result = 31 * result + arrivalPuck.hashCode()
        return result
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun toString(): String {
        return "LocationPuckOptions(" +
            "context=$context, " +
            "freeDrivePuck=$freeDrivePuck, " +
            "destinationPreviewPuck=$destinationPreviewPuck, " +
            "routePreviewPuck=$routePreviewPuck, " +
            "activeNavigationPuck=$activeNavigationPuck, " +
            "arrivalPuck=$arrivalPuck" +
            ")"
    }

    /**
     * Builder of [LocationPuckOptions]
     */
    class Builder(private val context: Context) {

        private var freeDrivePuck = regularPuck(context)
        private var destinationPreviewPuck = regularPuck(context)
        private var routePreviewPuck = regularPuck(context)
        private var activeNavigationPuck = navigationPuck(context)
        private var arrivalPuck = navigationPuck(context)

        /**
         * Apply the same [LocationPuck2D] or [LocationPuck3D] to location puck in all different
         * navigation states
         * @param defaultPuck [LocationPuck] to be used in all navigation states
         */
        fun defaultPuck(defaultPuck: LocationPuck): Builder = apply {
            this.freeDrivePuck = defaultPuck
            this.destinationPreviewPuck = defaultPuck
            this.routePreviewPuck = defaultPuck
            this.activeNavigationPuck = defaultPuck
            this.arrivalPuck = defaultPuck
        }

        /**
         * Apply [LocationPuck2D] or [LocationPuck3D] to location puck in free drive state
         * @param freeDrivePuck [LocationPuck] to be used in free drive state
         */
        fun freeDrivePuck(freeDrivePuck: LocationPuck): Builder = apply {
            this.freeDrivePuck = freeDrivePuck
        }

        /**
         * Apply [LocationPuck2D] or [LocationPuck3D] to location puck in destination preview state
         * @param destinationPreviewPuck [LocationPuck] to be used in destination preview state
         */
        fun destinationPreviewPuck(destinationPreviewPuck: LocationPuck): Builder = apply {
            this.destinationPreviewPuck = destinationPreviewPuck
        }

        /**
         * Apply [LocationPuck2D] or [LocationPuck3D] to location puck in route preview state
         * @param routePreviewPuck [LocationPuck] to be used in route preview state
         */
        fun routePreviewPuck(routePreviewPuck: LocationPuck): Builder = apply {
            this.routePreviewPuck = routePreviewPuck
        }

        /**
         * Apply [LocationPuck2D] or [LocationPuck3D] to location puck in active navigation state
         * @param activeNavigationPuck [LocationPuck] to be used in active navigation state
         */
        fun activeNavigationPuck(activeNavigationPuck: LocationPuck): Builder = apply {
            this.activeNavigationPuck = activeNavigationPuck
        }

        /**
         * Apply [LocationPuck2D] or [LocationPuck3D] to location puck in arrival state
         * @param arrivalPuck [LocationPuck] to be used in arrival state
         */
        fun arrivalPuck(arrivalPuck: LocationPuck): Builder = apply {
            this.arrivalPuck = arrivalPuck
        }

        /**
         * Build a new instance of [LocationPuckOptions]
         *
         * @return [LocationPuckOptions]
         */
        fun build(): LocationPuckOptions {
            return LocationPuckOptions(
                context,
                freeDrivePuck,
                destinationPreviewPuck,
                routePreviewPuck,
                activeNavigationPuck,
                arrivalPuck
            )
        }

        companion object {
            fun navigationPuck(context: Context): LocationPuck = LocationPuck2D(
                bearingImage = ContextCompat.getDrawable(
                    context,
                    R.drawable.mapbox_navigation_puck_icon,
                )
            )

            fun regularPuck(context: Context): LocationPuck = LocationPuck2D(
                topImage = ContextCompat.getDrawable(
                    context,
                    com.mapbox.maps.plugin.locationcomponent.R.drawable.mapbox_user_icon
                ),
                bearingImage = ContextCompat.getDrawable(
                    context,
                    com.mapbox.maps.plugin.locationcomponent.R.drawable.mapbox_user_bearing_icon
                ),
                shadowImage = ContextCompat.getDrawable(
                    context,
                    com.mapbox.maps.plugin.locationcomponent.R.drawable.mapbox_user_stroke_icon
                )
            )
        }
    }
}
