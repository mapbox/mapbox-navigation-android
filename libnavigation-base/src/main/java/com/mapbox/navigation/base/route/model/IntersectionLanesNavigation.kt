package com.mapbox.navigation.base.route.model

/**
 *
 * @property valid Boolean value for whether this lane can be taken to complete the maneuver. For
 * instance, if the lane array has four objects and the first two are marked as valid, then the
 * driver can take either of the left lanes and stay on the route.
 * @since 1.0
 *
 * @property indications Array of signs for each turn lane. There can be multiple signs. For example, a turning
 * lane can have a sign with an arrow pointing left and another sign with an arrow pointing
 * straight.
 * @since 1.0
 */
class IntersectionLanesNavigation(
    val valid: Boolean?,
    val indications: List<String>?
)
