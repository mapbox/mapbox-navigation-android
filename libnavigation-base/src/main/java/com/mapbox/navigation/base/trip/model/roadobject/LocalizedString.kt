package com.mapbox.navigation.base.trip.model.roadobject

/**
 * A wrapper that contains a string in a particular language and the info
 * about the corresponding language.
 *
 * @param language language which the string is in
 * @param value original string value
 */
class LocalizedString internal constructor(
    val language: String,
    val value: String,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LocalizedString

        if (language != other.language) return false
        if (value != other.value) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = language.hashCode()
        result = 31 * result + value.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "LocalizedString(language='$language', value='$value')"
    }
}
