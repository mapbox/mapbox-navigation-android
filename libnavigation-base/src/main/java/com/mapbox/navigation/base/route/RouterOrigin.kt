package com.mapbox.navigation.base.route

/**
 * Describes which kind of router presents response.
 */
sealed class RouterOrigin {
    /**
     * Router based on Directions API.
     *
     * See also [https://docs.mapbox.com/help/glossary/directions-api/]
     */
    object Offboard : RouterOrigin()

    /**
     * Router based on embedded offline library and local navigation tiles.
     */
    object Onboard : RouterOrigin()

    /**
     * Can be used as a router origin of custom routes source, different from [Offboard] and [Onboard]. The
     * SDK doesn't operate with [obj], it might be used to spread any additional data.
     *
     * @param obj is used to provide additional data with [Custom] router origin
     */
    data class Custom @JvmOverloads constructor(val obj: Any? = null) : RouterOrigin()
}
