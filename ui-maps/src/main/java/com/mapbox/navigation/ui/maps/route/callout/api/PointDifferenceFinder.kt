package com.mapbox.navigation.ui.maps.route.callout.api

import com.mapbox.geojson.Point
import kotlin.math.min

internal object PointDifferenceFinder {

    /**
     * Returns a subsequence of [altRoutePoints] having beginning and ending items removed if they are the same as
     * [primaryRoutePoints] have at the beginning and at the end
     *
     * If [firstIndexOfAlt] is provided then we can start iterating from that index to reduce unnecessary calculation
     */
    fun extractDifference(
        primaryRoutePoints: List<Point>,
        altRoutePoints: List<Point>,
        firstIndexOfAlt: Int?,
    ): List<Point> {
        // we use our custom implementation instead of TurfMisc.lineIntersect as:
        // - works precisely enough in production
        // - has less time complexity
        val rightBorder = findLastDifferentPointIndex(primaryRoutePoints, altRoutePoints)
        val leftBorder = findFirstDifferentPointIndex(
            primaryRoutePoints,
            altRoutePoints,
            firstIndexOfAlt.takeIf { it != null && rightBorder > it },
        )

        if (leftBorder == -1 || rightBorder == -1 || leftBorder > rightBorder) return emptyList()

        return altRoutePoints
            .subList(leftBorder, rightBorder + 1)
    }

    private fun findFirstDifferentPointIndex(
        primaryRoutePoints: List<Point>,
        altRoutePoints: List<Point>,
        firstIndexOfAlt: Int?,
    ): Int {
        val minimumLastIndex = min(primaryRoutePoints.lastIndex, altRoutePoints.lastIndex)
        val startIndex = firstIndexOfAlt.takeIf { it != null && minimumLastIndex > it } ?: 0

        val mainIterator = primaryRoutePoints.listIterator(startIndex)
        val altIterator = altRoutePoints.listIterator(startIndex)
        while (mainIterator.hasNext() && altIterator.hasNext()) {
            if (altIterator.next() != mainIterator.next()) {
                return altIterator.previousIndex()
            }
        }
        return -1
    }

    private fun findLastDifferentPointIndex(
        primaryRoutePoints: List<Point>,
        altRoutePoints: List<Point>,
    ): Int {
        val mainIterator = primaryRoutePoints.listIterator(primaryRoutePoints.size)
        val altIterator = altRoutePoints.listIterator(altRoutePoints.size)
        while (mainIterator.hasPrevious() && altIterator.hasPrevious()) {
            if (altIterator.previous() != mainIterator.previous()) {
                return altIterator.nextIndex()
            }
        }

        return -1
    }
}
