# Mapbox Electronic Horizon v3 integration document

To start listening the electronic horizon updates `EHorizonObserver` should be registered with

```kotlin
mapboxNavigation.registerEHorizonObserver(eHorizonObserver)
```

It provides the next callbacks:

```kotlin
/**
 * Electronic horizon listener. Callbacks are fired in the order specified.
 * onPositionUpdated might be called multiple times after the other callbacks until a new change to
 * the horizon occurs.
 */
interface EHorizonObserver {

    /**
     * This callback might be called multiple times when the position changes.
     * @param position current electronic horizon position (map matched position + e-horizon tree)
     * @param distances a list of [RoadObjectDistanceInfo] for upcoming road objects
     *
     */
    fun onPositionUpdated(
        position: EHorizonPosition,
        distances: List<RoadObjectDistanceInfo>
    )

    /**
     * Called when entry to line-like (i.e. which has length != null) road object was detected
     * @param objectEnterExitInfo contains info related to the object
     */
    fun onRoadObjectEnter(objectEnterExitInfo: RoadObjectEnterExitInfo)

    /**
     * Called when exit from line-like (i.e. which has length != null) road object was detected
     * @param objectEnterExitInfo contains info related to the object
     */
    fun onRoadObjectExit(objectEnterExitInfo: RoadObjectEnterExitInfo)

    /**
     * Called when the object is passed.
     * @param objectPassInfo contains info related to the object
     */
    fun onRoadObjectPassed(objectPassInfo: RoadObjectPassInfo)

    /**
     * This callback is fired whenever road object is added
     * @param roadObjectId id of the object
     */
    fun onRoadObjectAdded(roadObjectId: String)

    /**
     * This callback is fired whenever road object is updated
     * @param roadObjectId id of the object
     */
    fun onRoadObjectUpdated(roadObjectId: String)

    /**
     * This callback is fired whenever road object is removed
     * @param roadObjectId id of the object
     */
    fun onRoadObjectRemoved(roadObjectId: String)
}
```

Anytime you need edge's shape or metadata you can get it with `GraphAccessor`.
It's available with
```kotlin
mapboxNavigation.graphAccessor
```

```kotlin
class GraphAccessor {
    /**
     * Returns Graph Edge geometry for the given GraphId of the edge.
     * If edge with given edgeId is not accessible, returns null
     * @param edgeId
     *
     * @return list of Points representing edge shape
     */
    fun getEdgeShape(edgeId: Long): List<Point>?

    /**
     * Returns Graph Edge meta-information for the given GraphId of the edge.
     * If edge with given edgeId is not accessible, returns null
     * @param edgeId
     *
     * @return EHorizonEdgeMetadata
     */
    fun getEdgeMetadata(edgeId: Long): EHorizonEdgeMetadata?

    /**
     * Returns geometry of given path on graph.
     * If any of path edges is not accessible, returns null.
     */
    fun getPathShape(graphPath: EHorizonGraphPath): List<Point>?

    /**
     * Returns geographical coordinate of given position on graph
     * If position's edge is not accessible, returns null.
     */
    fun getGraphPositionCoordinate(graphPosition: EHorizonGraphPosition): Point?
}
```

Anytime you need a road object, upcoming road objects, add or remove custom road object you can get it with `RoadObjectsStore`
It's available with
```kotlin
mapboxNavigation.roadObjectsStore
```

```kotlin
class RoadObjectsStore {
    /**
     * Returns mapping `road object id -> EHorizonObjectEdgeLocation` for all road objects
     * which are lying on the edge with given id.
     * @param edgeId
     */
    fun getRoadObjectsOnTheEdge(edgeId: Long): Map<String, EHorizonObjectEdgeLocation>

    /**
     * Returns roadObject, if such object cannot be found returns null.
     * @param roadObjectId id of the object
     */
    fun getRoadObject(roadObjectId: String): RoadObject?    

    /**
     * Returns list of road object ids which are (partially) belong to `edgeIds`.
     * @param edgeIds list of edge ids
     *
     * @return list of road object ids
     */
    fun getRoadObjectIdsByEdgeIds(edgeIds: List<Long>): List<String>

    /**
     * Adds road object to be tracked in electronic horizon. In case if object with such id already
     * exists updates it.
     * @param roadObject object to add
     */
    fun addCustomRoadObject(roadObject: RoadObject)

    /**
     * Removes custom road object (i.e. stops tracking it in electronic horizon)
     * @param roadObjectId of road object
     */
    fun removeCustomRoadObject(roadObjectId: String)
    
    /**
     * Removes all custom road objects (i.e. stops tracking them in electronic horizon)
     */
    fun removeAllCustomRoadObjects() {
        navigator.roadObjectsStore?.removeAllCustomRoadObjects()
    }
    
    /**
    * Returns a list of [UpcomingRoadObject]
    * @param distances a list of [RoadObjectDistanceInfo]
    */
    fun getUpcomingRoadObjects(
        distances: List<RoadObjectDistanceInfo>
    ): List<UpcomingRoadObject>
}
```

If you want to track your own object (represented as a polygon, polyline, point, gantry or encoded with OpenLR)
with the electronic horizon, you should use `RoadObjectMatcher` to match you object to `RoadObject`.

Implement `RoadObjectMatcherObserver` 
```kotlin
/**
 * Road objects matching listener. Callbacks are fired when matching is finished.
 */
interface RoadObjectMatcherObserver {
    /**
     * Road object matching result.
     */
    fun onRoadObjectMatched(result: Expected<RoadObject, RoadObjectMatcherError>)
}
```
Register the observer
```kotlin
mapboxNavigation.roadObjectMatcher.registerRoadObjectMatcherObserver(observer)
```
Match your object with one of the methods
```kotlin
/**
 * [MapboxNavigation.roadObjectMatcher] provides methods to match custom road objects.
 */
class RoadObjectMatcher {
    /**
     * Matches given OpenLR object to the graph.
     * @param roadObjectId unique id of the object
     * @param openLRLocation road object location
     * @param openLRStandard standard used to encode openLRLocation
     * In case of error (if there are no tiles in cache, decoding failed, etc.)
     * object won't be matched.
     */
    fun matchOpenLRObject(
        roadObjectId: String,
        openLRLocation: String,
        @OpenLRStandard.Type openLRStandard: String
    )
    
    /**
     * Matches given polyline to graph.
     * Polyline should define valid path on graph,
     * i.e. it should be possible to drive this path according to traffic rules.
     * In case of error (if there are no tiles in cache, decoding failed, etc.)
     * object won't be matched.
     *
     * @param roadObjectId unique id of the object
     * @param polyline polyline representing the object
     */
    fun matchPolylineObject(roadObjectId: String, polyline: List<Point>) {
        navigator.roadObjectMatcher?.matchPolyline(polyline, roadObjectId)
    }

    /**
     * Matches given polygon to graph.
     * "Matching" here means we try to find all intersections of polygon with the road graph
     * and track distances to those intersections as distance to polygon.
     * In case of error (if there are no tiles in cache, decoding failed, etc.)
     * object won't be matched.
     *
     * @param roadObjectId unique id of the object
     * @param polygon polygon representing the object
     */
    fun matchPolygonObject(roadObjectId: String, polygon: List<Point>) {
        navigator.roadObjectMatcher?.matchPolygon(polygon, roadObjectId)
    }

    /**
     * Matches given gantry (i.e. polyline orthogonal to the road) to the graph.
     * "Matching" here means we try to find all intersections of gantry with road graph
     * and track distances to those intersections as distance to gantry.
     * In case of error (if there are no tiles in cache, decoding failed, etc.)
     * object won't be matched.
     *
     * @param roadObjectId unique id of the object
     * @param gantry gantry representing the object
     */
    fun matchGantryObject(roadObjectId: String, gantry: List<Point>) {
        navigator.roadObjectMatcher?.matchGantry(gantry, roadObjectId)
    }

    /**
     * Matches given point to road graph.
     * In case of error (if there are no tiles in cache, decoding failed, etc.)
     * object won't be matched.
     *
     * @param roadObjectId unique id of the object
     * @param point point representing the object
     */
    fun matchPointObject(roadObjectId: String, point: Point) {
        navigator.roadObjectMatcher?.matchPoint(point, roadObjectId)
    }

    /**
     * Cancel road object matching
     *
     * @param roadObjectId unique id of the object
     */
    fun cancel(roadObjectId: String) {
        navigator.roadObjectMatcher?.cancel(roadObjectId)
    }
}
```
You will receive matching result in the `RoadObjectMatcherObserver`. 

If the result is success, add matched `RoadObject` to the electronic horizon with `RoadObjectStore.addCustomRoadObject`