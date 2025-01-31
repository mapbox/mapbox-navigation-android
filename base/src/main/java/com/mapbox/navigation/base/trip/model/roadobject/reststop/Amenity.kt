package com.mapbox.navigation.base.trip.model.roadobject.reststop

/**
 * Provides information about amenities available at a given [RestStop]
 *
 * @param name name of the amenity
 * @param type type of the amenity. See [AmenityType]
 * @param brand brand of the amenity.
 */
class Amenity internal constructor(
    @AmenityType.Type val type: String,
    val name: String?,
    val brand: String?,
) {
    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Amenity

        if (type != other.type) return false
        if (name != other.name) return false
        if (brand != other.brand) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + (name.hashCode())
        result = 31 * result + (brand.hashCode())
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "Amenity(" +
            "type=$type, " +
            "name=$name, " +
            "brand=$brand)"
    }
}
