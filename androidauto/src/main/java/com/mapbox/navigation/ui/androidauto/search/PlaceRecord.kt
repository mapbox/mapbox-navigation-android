package com.mapbox.navigation.ui.androidauto.search

import com.mapbox.geojson.Point

/**
 * Use the [PlaceRecordMapper]
 */
class PlaceRecord(
    val id: String,
    val name: String,
    val coordinate: Point?,
    val description: String? = null,
    val categories: List<String> = listOf(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PlaceRecord

        if (id != other.id) return false
        if (name != other.name) return false
        if (coordinate != other.coordinate) return false
        if (description != other.description) return false
        if (categories != other.categories) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + (coordinate?.hashCode() ?: 0)
        result = 31 * result + (description?.hashCode() ?: 0)
        result = 31 * result + categories.hashCode()
        return result
    }

    override fun toString(): String {
        return "PlaceRecord(" +
            "id='$id', " +
            "name='$name', " +
            "coordinate=$coordinate, " +
            "description=$description, " +
            "categories=$categories" +
            ")"
    }
}
