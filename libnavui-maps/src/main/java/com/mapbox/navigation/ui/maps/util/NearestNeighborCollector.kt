package com.mapbox.navigation.ui.maps.util

import com.mapbox.geojson.Point
import com.mapbox.navigation.ui.utils.internal.ifNonNull
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import java.util.Collections
import java.util.PriorityQueue
import java.util.function.Supplier

internal class NearestNeighborCollector<T: Supplier<Point>>(
    val queryPoint: Point,
    private val capacity: Int
) {

    private val distanceComparator by lazy {
        DistanceComparator(queryPoint)
    }

    private val priorityQueue by lazy {
        PriorityQueue<T>(capacity, Collections.reverseOrder(distanceComparator))
    }

    private var distanceToFarthestPoint: Double = 0.0

    fun offerPoint(offeredPoint: T) {
        val pointAdded = if (priorityQueue.size < this.capacity) {
            priorityQueue.add(offeredPoint)
        } else {
            if (priorityQueue.isNotEmpty()) {
                val distanceToNewPoint = TurfMeasurement.distance(
                    queryPoint,
                    offeredPoint.get(),
                    TurfConstants.UNIT_METERS
                )
                if (distanceToNewPoint < distanceToFarthestPoint) {
                    priorityQueue.poll()
                    priorityQueue.add(offeredPoint)
                    true
                } else {
                    false
                }
            } else {
                false
            }
        }

        if (pointAdded && priorityQueue.isNotEmpty()) {
            distanceToFarthestPoint =
                ifNonNull(priorityQueue.peek()) { pointSupplier: Supplier<Point>  ->
                    TurfMeasurement.distance(
                        queryPoint,
                        pointSupplier.get(),
                        TurfConstants.UNIT_METERS
                    )
            } ?: Double.MAX_VALUE
        }
    }

    fun getFarthestPoint(): Point? = priorityQueue.peek()?.get()

    fun toSortedList() = priorityQueue.toList().sortedWith(distanceComparator)
}
