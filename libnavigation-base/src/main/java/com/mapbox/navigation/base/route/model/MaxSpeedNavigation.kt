package com.mapbox.navigation.base.route.model

/**
 *
 * @property speed Number indicating the posted speed limit.
 * @since 1.0
 *
 * @property unit String indicating the unit of speed, either as `km/h` or `mph`.
 * @since 1.0
 *
 * @property unknown Boolean is true if the speed limit is not known, otherwise null.
 * @since 1.0
 *
 * @property none Boolean is `true` if the speed limit is unlimited, otherwise null.
 * @since 1.0
 */
class MaxSpeedNavigation(
    val speed: Int?,
    val unit: String?,
    val unknown: Boolean?,
    val none: Boolean?
)
