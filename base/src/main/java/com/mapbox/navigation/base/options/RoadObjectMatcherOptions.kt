package com.mapbox.navigation.base.options

import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.options.NavigationTileDataDomain.NAVIGATION

/**
 * Configures the road object matching behavior.
 *
 * If no specific options are provided, default values will be utilized.
 *
 * @param openLRMaxDistanceToNode Specifies the maximum search distance (in meters) from a Linear
 * Referencing Point (LRP) to a graph node when matching road objects.
 * This value directly influences the number of graph candidates fetched around an LRP.
 * - A **low value** might prevent finding the nearest graph candidates, potentially leading to matching failures.
 * - A **high value** can negatively impact performance due to increased processing of candidates.
 * @param matchingGraphType Determines the type of graph data used for matching road objects.
 * See [NavigationTileDataDomain] for available types.
 */
@OptIn(ExperimentalMapboxNavigationAPI::class)
class RoadObjectMatcherOptions private constructor(
    val openLRMaxDistanceToNode: Double?,
    val matchingGraphType: NavigationTileDataDomain,
) {

    /**
     * @return builder matching the one used to create this instance
     */
    fun toBuilder(): Builder = Builder().apply {
        openLRMaxDistanceToNode(openLRMaxDistanceToNode)
        matchingGraphType(matchingGraphType)
    }

    /**
     * Build a new [RoadObjectMatcherOptions]
     */
    class Builder {
        private var openLRMaxDistanceToNode: Double? = null
        private var matchingGraphType: NavigationTileDataDomain = NAVIGATION

        /**
         * Change the [openLRMaxDistanceToNode]
         *
         * Note that the recommended approach is to keep the [newMaxDistance] within the range of:
         * [8, 70] meters. With those values bing subject to future changes.
         */
        fun openLRMaxDistanceToNode(newMaxDistance: Double?): Builder =
            apply { this.openLRMaxDistanceToNode = newMaxDistance }

        /**
         * Change the [NavigationTileDataDomain]
         */
        fun matchingGraphType(newType: NavigationTileDataDomain): Builder =
            apply { this.matchingGraphType = newType }

        /**
         * Build the [RoadObjectMatcherOptions]
         */
        fun build() = RoadObjectMatcherOptions(
            openLRMaxDistanceToNode = openLRMaxDistanceToNode,
            matchingGraphType = matchingGraphType,
        )
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RoadObjectMatcherOptions

        if (openLRMaxDistanceToNode != other.openLRMaxDistanceToNode) return false
        if (matchingGraphType != other.matchingGraphType) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = openLRMaxDistanceToNode.hashCode()
        result = 31 * result + matchingGraphType.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object
     */
    override fun toString(): String {
        return "RoadObjectMatcherOptions(openLRMaxDistanceToNode='$openLRMaxDistanceToNode'," +
            " matchingGraphType=$matchingGraphType)"
    }
}

/**
 * Describes the tiles data domain to be used for matching road objects.
 */
enum class NavigationTileDataDomain {
    /** Data for Maps */
    MAPS,

    /** Data for Navigation */
    NAVIGATION,

    /** Data for Search */
    SEARCH,

    /** Data for ADAS */
    ADAS,

    /** Data for Navigation HD */
    NAVIGATION_HD,
}
