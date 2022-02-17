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
     * Can be used as a router origin of custom routes source, different from [Offboard] and [Onboard].
     * @param obj is used to provide additional data with [Custom] router origin
     */
    data class Custom
    @Deprecated(
        "`Nullable obj` is not supported anymore, `obj` becomes `null` when " +
            "it's passed in custom Router.",
        ReplaceWith("RouterOrigin.Custom()")
    )
    constructor(
        @Deprecated(
            "`obj` is not supported anymore, it becomes `null` when passed " +
                "in custom Router."
        )
        val obj: Any? = null
    ) : RouterOrigin() {
        constructor() : this(null)
    }
}
