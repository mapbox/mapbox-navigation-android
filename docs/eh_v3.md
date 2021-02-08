# EH_v3 integration document

To start listening EHorizon updates `EHorizonObserver` should be registered with

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
     * @param position current electronic horizon position(map matched position + e-horizon tree)
     * @param distances map road object id -> EHorizonObjectDistanceInfo for upcoming road objects
     *
     */
    fun onPositionUpdated(
        position: EHorizonPosition,
        distances: Map<String, EHorizonObjectDistanceInfo>
    )

    /**
     * Called when entry to line-like(i.e. which has length != null) road object was detected
     * @param objectEnterExitInfo contains info related to the object
     */
    fun onRoadObjectEnter(objectEnterExitInfo: EHorizonObjectEnterExitInfo)

    /**
     * Called when exit from line-like(i.e. which has length != null) road object was detected
     * @param objectEnterExitInfo contains info related to the object
     */
    fun onRoadObjectExit(objectEnterExitInfo: EHorizonObjectEnterExitInfo)

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

Anytime you need edge's shape or metadata you can get it with `EHorizonGraphAccessor`.
It's available with
```kotlin
mapboxNavigation.getEHorizonGraphAccessor()
```

```kotlin
interface EHorizonGraphAccessor {
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
}
```

Anytime you need road object's metadata or location you can get it with `EHorizonObjectsStore`
It's available with
```kotlin
mapboxNavigation.getEHorizonObjectsStore()
```

```kotlin
interface EHorizonObjectsStore {
    /**
     * Returns mapping `road object id -> EHorizonObjectEdgeLocation` for all road objects
     * which are lying on the edge with given id.
     * @param edgeId
     */
    fun getRoadObjectsOnTheEdge(edgeId: Long): Map<String, EHorizonObjectEdgeLocation>

    /**
     * Returns metadata of object with given id, if such object cannot be found returns null.
     * @param roadObjectId
     */
    fun getRoadObjectMetadata(roadObjectId: String): EHorizonObjectMetadata?

    /**
     * Returns location of object with given id, if such object cannot be found returns null.
     * @param roadObjectId
     */
    fun getRoadObjectLocation(roadObjectId: String): EHorizonObjectLocation?

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
     * @param roadObjectId unique id of the object
     * @param openLRLocation road object location
     * @param standard standard used to encode openLRLocation
     */
    fun addCustomRoadObject(roadObjectId: String, openLRLocation: String, standard: OpenLRStandard)

    /**
     * Removes road object(i.e. stops tracking it in electronic horizon)
     * @param roadObjectId of road object
     */
    fun removeCustomRoadObject(roadObjectId: String)
}
```