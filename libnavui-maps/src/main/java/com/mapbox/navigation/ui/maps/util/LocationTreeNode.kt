package com.mapbox.navigation.ui.maps.util

import com.mapbox.geojson.Point
import java.util.function.Supplier

internal class LocationTreeNode<T: Supplier<Point>>(
    private val points: MutableList<T>,
    private val capacity: Int = 10,
    private val distanceCalcFunction: (Point, Point) ->  Double
) {

    private val vantagePoint: Point by lazy {
        points.random().get()
    }
    private var threshold = 0.0
    private var closer: LocationTreeNode<T>? = null
    private var farther: LocationTreeNode<T>? = null

    init {
        initializeNode()
    }

    internal fun initializeNode() {
        if (points.isEmpty()) {
            if (closer?.size() == 0 || farther?.size() == 0) {
                // Prune empty child nodes.
                addAllPointsToCollection(points)
                closer = null
                farther = null
                initializeNode()
            } else {
                closer?.initializeNode()
                farther?.initializeNode()
            }
        } else {
            // What matters is that the points with in the distance threshold of the
            // vantage point are on the left of the vantage point index in the list.
            points.sortBy { distanceCalcFunction(vantagePoint, it.get()) }
            if (points.size > capacity) {
                threshold = distanceCalcFunction(vantagePoint, points[points.size / 2].get())
                when (val firstPastThreshold =
                    partitionPoints(vantagePoint, points.map { it.get() }, threshold)) {
                    in 0 .. Int.MAX_VALUE -> {
                        closer = LocationTreeNode(
                            points.subList(0, firstPastThreshold).toMutableList(),
                            capacity,
                            distanceCalcFunction
                        )
                        farther = LocationTreeNode(
                            points.subList(firstPastThreshold, points.size).toMutableList(),
                            capacity,
                            distanceCalcFunction
                        )
                        points.clear()
                    }
                    else -> {
                        closer = null
                        farther = null
                    }
                }
            }
        }
    }

    fun size(): Int {
        return if (this.points.isEmpty()) {
            (closer?.size() ?: 0) + (farther?.size() ?: 0)
        } else {
            this.points.size
        }
    }

    fun add(point: T) {
        if (points.isEmpty()) {
            getChildNodeForPoint(point.get())?.add(point)
        } else {
            points.add(point)
        }
    }

    fun remove(point: T): Boolean {
        return if (points.isEmpty()) {
            getChildNodeForPoint(point.get())?.remove(point) ?: false
        } else {
            points.remove(point)
        }
    }

    fun collectNearestNeighbors(collector: NearestNeighborCollector<T>) {
        if (points.isEmpty()) {
            val firstNodeSearched = getChildNodeForPoint(collector.queryPoint)
            firstNodeSearched?.collectNearestNeighbors(collector)

            val distanceFromVantagePointToQueryPoint = distanceCalcFunction(
                vantagePoint,
                collector.queryPoint
            )
            val distanceFromQueryPointToFarthestPoint = if (collector.getFarthestPoint() != null) {
                distanceCalcFunction(collector.queryPoint, collector.getFarthestPoint()!!)
            } else {
                Double.MAX_VALUE
            }

            if (firstNodeSearched == closer) {
                val distanceFromQueryPointToThreshold =
                    threshold - distanceFromVantagePointToQueryPoint

                if (distanceFromQueryPointToFarthestPoint > distanceFromQueryPointToThreshold) {
                    farther?.collectNearestNeighbors(collector)
                }
            } else {
                val distanceFromQueryPointToThreshold =
                    distanceFromVantagePointToQueryPoint - threshold

                if (distanceFromQueryPointToThreshold <= distanceFromQueryPointToFarthestPoint) {
                    closer?.collectNearestNeighbors(collector)
                }
            }
        } else {
            points.forEach {
                collector.offerPoint(it)
            }
        }
    }

    private fun getChildNodeForPoint(point: Point): LocationTreeNode<T>? {
        return if (distanceCalcFunction(vantagePoint, point)  <= threshold) {
            closer
        } else {
            farther
        }
    }

    private fun addAllPointsToCollection(collection: MutableList<T>) {
        if (points.isEmpty()) {
            closer?.addAllPointsToCollection(collection)
            farther?.addAllPointsToCollection(collection)
        } else {
            collection.addAll(points)
        }
    }

    private fun partitionPoints(vantagePoint: Point, points: List<Point>, threshold: Double): Int {
        return points.map {
            Pair(it, distanceCalcFunction(vantagePoint, it))
        }.indexOfFirst { it.second > threshold }
    }
}
