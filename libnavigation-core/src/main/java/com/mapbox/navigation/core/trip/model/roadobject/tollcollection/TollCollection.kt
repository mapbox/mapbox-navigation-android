package com.mapbox.navigation.core.trip.model.roadobject.tollcollection

import com.mapbox.navigation.base.trip.model.roadobject.RoadObject
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectGeometry
import com.mapbox.navigation.core.trip.model.roadobject.RoadObjectType

/**
 * Road object type that provides information about toll collection points on the route.
 *
 * @param tollCollectionType information about a toll collection point. See [TollCollectionType].
 * @see RoadObject
 * @see RoadObjectType.TOLL_COLLECTION
 */
class TollCollection private constructor(
    distanceFromStartOfRoute: Double?,
    objectGeometry: RoadObjectGeometry,
    @TollCollectionType.Type val tollCollectionType: Int
) : RoadObject(
    RoadObjectType.TOLL_COLLECTION,
    distanceFromStartOfRoute,
    objectGeometry
) {

    /**
     * Transform this object into a builder to mutate the values.
     */
    fun toBuilder(): Builder = Builder(
        objectGeometry,
        tollCollectionType
    )
        .distanceFromStartOfRoute(distanceFromStartOfRoute)

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as TollCollection

        if (tollCollectionType != other.tollCollectionType) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + tollCollectionType
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "TollCollectionAlert(tollCollectionType=$tollCollectionType), ${super.toString()}"
    }

    /**
     * Use to create a new instance.
     *
     * @see TollCollection
     */
    class Builder(
        private val objectGeometry: RoadObjectGeometry,
        @TollCollectionType.Type
        private val tollCollectionType: Int
    ) {
        private var distanceFromStartOfRoute: Double? = null

        /**
         * Add optional distance from start of route.
         * If [distanceFromStartOfRoute] is negative, `null` will be used.
         */
        fun distanceFromStartOfRoute(distanceFromStartOfRoute: Double?): Builder = apply {
            this.distanceFromStartOfRoute = distanceFromStartOfRoute
        }

        /**
         * Build the object instance.
         */
        fun build() =
            TollCollection(
                distanceFromStartOfRoute,
                objectGeometry,
                tollCollectionType
            )
    }
}
