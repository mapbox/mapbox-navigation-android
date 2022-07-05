package com.mapbox.navigation.core.trip.roadobject.reststop

import com.mapbox.api.directions.v5.models.Amenity
import com.mapbox.api.directions.v5.models.RestStop
import com.mapbox.api.directions.v5.models.StepIntersection
import com.mapbox.geojson.Point

/**
 * The class provides access to [RestStop] available in [StepIntersection].
 *
 * @param location location of the rest stop
 * @param name name of the rest stop
 * @param type type of the rest stop
 * @param amenities facilities available in the rest stop. See [Amenity]
 */
class RestStopFromIntersection private constructor(
    val location: Point,
    val name: String?,
    val type: String?,
    val amenities: List<Amenity>?,
) {

    /**
     * @return the [Builder] that created the [RestStopFromIntersection]
     */
    fun toBuilder(): Builder = Builder(location).apply {
        name(name)
        type(type)
        amenities(amenities)
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RestStopFromIntersection

        if (location != other.location) return false
        if (name != other.name) return false
        if (type != other.type) return false
        if (amenities != other.amenities) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = location.hashCode()
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (type?.hashCode() ?: 0)
        result = 31 * result + (amenities?.hashCode() ?: 0)
        return result
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun toString(): String {
        return "RestStopFromIntersection(" +
            "location=$location, " +
            "name=$name, " +
            "type=$type, " +
            "amenities=$amenities" +
            ")"
    }

    /**
     * Builder of [RestStopFromIntersection]
     *
     * @param location location of the [RestStop]
     */
    class Builder(private val location: Point) {

        private var name: String? = null
        private var type: String? = null
        private var amenities: List<Amenity>? = null

        /**
         * Apply the name of the rest stop.
         *
         * @param name name of the rest stop
         */
        fun name(name: String?): Builder =
            apply { this.name = name }

        /**
         * Apply the type of the rest stop.
         *
         * @param type type of the rest stop
         */
        fun type(type: String?): Builder =
            apply { this.type = type }

        /**
         * Apply the amenities of the rest stop.
         *
         * @param amenities amenities of the rest stop
         */
        fun amenities(amenities: List<Amenity>?): Builder =
            apply { this.amenities = amenities }

        /**
         * Build an instance of [RestStopFromIntersection]
         */
        fun build(): RestStopFromIntersection {
            return RestStopFromIntersection(
                location = location,
                name = name,
                type = type,
                amenities = amenities,
            )
        }
    }
}
