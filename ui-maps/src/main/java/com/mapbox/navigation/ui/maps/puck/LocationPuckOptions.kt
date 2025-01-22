package com.mapbox.navigation.ui.maps.puck

import android.content.Context
import androidx.core.content.ContextCompat
import com.mapbox.maps.ImageHolder
import com.mapbox.maps.plugin.LocationPuck
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.LocationPuck3D
import com.mapbox.navigation.ui.maps.R
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import com.mapbox.navigation.ui.maps.puck.LocationPuckOptions.Builder
import com.mapbox.navigation.ui.utils.internal.extensions.getBitmap
import com.mapbox.navigation.ui.utils.internal.extensions.withBlurEffect

/**
 * Gives options to specify either [LocationPuck2D] or [LocationPuck3D] references to the location
 * puck to be displayed on top of map view in each of different navigation states.
 *
 * If you want to use the same puck for all different navigation states you can invoke
 * `defaultPuck()` on the [Builder] and that would be used for pucks in all navigation states.
 *
 * @param context from which to reference puck drawables
 * @param freeDrivePuck stores the puck appearance to be displayed in free drive state when in the [NavigationCameraState.FOLLOWING]
 * @param destinationPreviewPuck stores the puck appearance to be displayed in destination preview state when in the [NavigationCameraState.FOLLOWING]
 * @param routePreviewPuck stores the puck appearance to be displayed in route preview state when in the [NavigationCameraState.FOLLOWING]
 * @param activeNavigationPuck stores the puck appearance to be displayed in active navigation state when in the [NavigationCameraState.FOLLOWING]
 * @param arrivalPuck stores the puck appearance to be displayed in arrival state when in the [NavigationCameraState.FOLLOWING]
 * @param idlePuck stores the puck appearance to be displayed for all navigation states when in the [NavigationCameraState.IDLE] or the [NavigationCameraState.OVERVIEW]
 */
class LocationPuckOptions private constructor(
    val context: Context,
    val freeDrivePuck: LocationPuck,
    val destinationPreviewPuck: LocationPuck,
    val routePreviewPuck: LocationPuck,
    val activeNavigationPuck: LocationPuck,
    val arrivalPuck: LocationPuck,
    val idlePuck: LocationPuck,
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
        idlePuck(idlePuck)
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
        if (idlePuck != other.idlePuck) return false

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
        result = 31 * result + idlePuck.hashCode()
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
            "idlePuck=$idlePuck" +
            ")"
    }

    /**
     * Builder of [LocationPuckOptions]
     */
    class Builder(private val context: Context) {

        private var freeDrivePuck: LocationPuck
        private var destinationPreviewPuck: LocationPuck
        private var routePreviewPuck: LocationPuck
        private var activeNavigationPuck: LocationPuck
        private var arrivalPuck: LocationPuck
        private var idlePuck: LocationPuck

        init {
            val navigationPuck = navigationPuck(context)
            val regularPuck = regularPuck()

            freeDrivePuck = navigationPuck
            destinationPreviewPuck = navigationPuck
            routePreviewPuck = navigationPuck
            activeNavigationPuck = navigationPuck
            arrivalPuck = navigationPuck
            idlePuck = regularPuck
        }

        /**
         * Apply the same [LocationPuck2D] or [LocationPuck3D] to location puck for all navigation states
         * and any [NavigationCameraState]
         * @param defaultPuck [LocationPuck] to be used in all states
         */
        fun defaultPuck(defaultPuck: LocationPuck): Builder = apply {
            this.freeDrivePuck = defaultPuck
            this.destinationPreviewPuck = defaultPuck
            this.routePreviewPuck = defaultPuck
            this.activeNavigationPuck = defaultPuck
            this.arrivalPuck = defaultPuck
            this.idlePuck = defaultPuck
        }

        /**
         * Apply [LocationPuck2D] or [LocationPuck3D] to location puck in free drive state
         * when in the [NavigationCameraState.FOLLOWING]
         * @param freeDrivePuck [LocationPuck] to be used in free drive state
         */
        fun freeDrivePuck(freeDrivePuck: LocationPuck): Builder = apply {
            this.freeDrivePuck = freeDrivePuck
        }

        /**
         * Apply [LocationPuck2D] or [LocationPuck3D] to location puck in destination preview state
         * when in the [NavigationCameraState.FOLLOWING]
         * @param destinationPreviewPuck [LocationPuck] to be used in destination preview state
         */
        fun destinationPreviewPuck(destinationPreviewPuck: LocationPuck): Builder = apply {
            this.destinationPreviewPuck = destinationPreviewPuck
        }

        /**
         * Apply [LocationPuck2D] or [LocationPuck3D] to location puck in route preview state
         * when in the [NavigationCameraState.FOLLOWING]
         * @param routePreviewPuck [LocationPuck] to be used in route preview state
         */
        fun routePreviewPuck(routePreviewPuck: LocationPuck): Builder = apply {
            this.routePreviewPuck = routePreviewPuck
        }

        /**
         * Apply [LocationPuck2D] or [LocationPuck3D] to location puck in active navigation state
         * when in the [NavigationCameraState.FOLLOWING]
         * @param activeNavigationPuck [LocationPuck] to be used in active navigation state
         */
        fun activeNavigationPuck(activeNavigationPuck: LocationPuck): Builder = apply {
            this.activeNavigationPuck = activeNavigationPuck
        }

        /**
         * Apply [LocationPuck2D] or [LocationPuck3D] to location puck in arrival state
         * when in the [NavigationCameraState.FOLLOWING]
         * @param arrivalPuck [LocationPuck] to be used in arrival state
         */
        fun arrivalPuck(arrivalPuck: LocationPuck): Builder = apply {
            this.arrivalPuck = arrivalPuck
        }

        /**
         * Apply [LocationPuck2D] or [LocationPuck3D] to location puck for all navigation states when
         * in the [NavigationCameraState.IDLE] or the [NavigationCameraState.OVERVIEW].
         * @param idlePuck [LocationPuck] to be used when in the [NavigationCameraState.IDLE]
         * or [NavigationCameraState.OVERVIEW]
         */
        fun idlePuck(idlePuck: LocationPuck): Builder = apply {
            this.idlePuck = idlePuck
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
                arrivalPuck,
                idlePuck,
            )
        }

        companion object {
            /**
             * Provides access to [LocationPuck2D] more suited for the [NavigationCameraState.FOLLOWING].
             */
            fun navigationPuck(context: Context): LocationPuck = LocationPuck2D(
                bearingImage = ImageHolder.from(R.drawable.mapbox_navigation_puck_icon2),
                shadowImage = ContextCompat.getDrawable(
                    context,
                    R.drawable.mapbox_navigation_puck_icon2_shadow,
                )
                    ?.withBlurEffect(context, 7.5f)
                    ?.getBitmap()
                    ?.let { ImageHolder.from(it) },
            )

            /**
             * Provides access to [LocationPuck2D] more suited for the [NavigationCameraState.IDLE]
             * and [NavigationCameraState.OVERVIEW].
             */
            fun regularPuck(): LocationPuck = LocationPuck2D(
                topImage = ImageHolder.from(
                    com.mapbox.maps.plugin.locationcomponent.R.drawable.mapbox_user_icon,
                ),
                bearingImage = ImageHolder.from(
                    com.mapbox.maps.plugin.locationcomponent.R.drawable.mapbox_user_bearing_icon,
                ),
                shadowImage = ImageHolder.from(
                    com.mapbox.maps.plugin.locationcomponent.R.drawable.mapbox_user_stroke_icon,
                ),
            )
        }
    }
}
