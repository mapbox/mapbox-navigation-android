package com.mapbox.navigation.base.road.model

/**
 * Object that holds road properties
 * @property name of the road if available otherwise null
 * @property shieldUrl url for the route shield if available otherwise null
 * @property shieldName name of the route shield if available otherwise null
 */
data class Road(
    val name: String? = null,
    val shieldUrl: String? = null,
    val shieldName: String? = null
)
