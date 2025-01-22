package com.mapbox.navigation.tripdata.maneuver.model

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import java.util.UUID

/**
 * "sub": {
 *      "components": [
 *          {
 *              "imageBaseURL": "https://mapbox-navigation-shields...",
 *              "type": "icon",
 *              "text": "CA 262"
 *          },
 *          {
 *              "type": "delimiter",
 *              "text": "/"
 *          },
 *          {
 *              "type": "text",
 *              "text": "Mission Boulevard"
 *          }
 *      ],
 *      "type": "turn",
 *      "modifier": "uturn",
 *      "text": "CA 262 / Mission Boulevard"
 * }
 *
 * A simplified data structure representing [BannerInstructions.sub]
 * @property id String A unique id
 * @property text String Plain text with all the [BannerComponents] text combined.
 * @property type String? indicates the type of maneuver.
 * @property degrees Double? degrees at which you will be exiting a roundabout.
 * @property modifier String? indicates the mode of the maneuver.
 * @property drivingSide String? represents which side of the street people drive on
 * in that location. Can be 'left' or 'right'.
 * @property componentList List<Component> a part or element of [BannerComponents]
 * @constructor
 */
class SubManeuver internal constructor(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val type: String? = null,
    val degrees: Double? = null,
    val modifier: String? = null,
    val drivingSide: String? = null,
    val componentList: List<Component> = listOf(),
) {

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SubManeuver

        if (id != other.id) return false
        if (text != other.text) return false
        if (type != other.type) return false
        if (degrees != other.degrees) return false
        if (modifier != other.modifier) return false
        if (drivingSide != other.drivingSide) return false
        if (componentList != other.componentList) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (text.hashCode())
        result = 31 * result + type.hashCode()
        result = 31 * result + degrees.hashCode()
        result = 31 * result + modifier.hashCode()
        result = 31 * result + drivingSide.hashCode()
        result = 31 * result + componentList.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "SubManeuver(" +
            "text='$text', " +
            "type=$type, " +
            "degrees=$degrees, " +
            "modifier=$modifier, " +
            "drivingSide=$drivingSide, " +
            "componentList=$componentList" +
            ")"
    }
}
