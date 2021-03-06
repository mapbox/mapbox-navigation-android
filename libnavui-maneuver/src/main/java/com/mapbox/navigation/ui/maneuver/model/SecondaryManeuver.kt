package com.mapbox.navigation.ui.maneuver.model

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import java.util.UUID

/**
 * "secondary": {
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
 * A simplified data structure representing [BannerInstructions.secondary]
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
class SecondaryManeuver private constructor(
    val id: String,
    val text: String,
    val type: String? = null,
    val degrees: Double? = null,
    val modifier: String? = null,
    val drivingSide: String? = null,
    val componentList: List<Component>
) {

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SecondaryManeuver

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
        result = 31 * result + (type?.hashCode() ?: 0)
        result = 31 * result + (degrees?.hashCode() ?: 0)
        result = 31 * result + (modifier?.hashCode() ?: 0)
        result = 31 * result + (drivingSide?.hashCode() ?: 0)
        result = 31 * result + componentList.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "SecondaryManeuver(" +
            "id='$id', " +
            "text='$text', " +
            "type=$type, " +
            "degrees=$degrees, " +
            "modifier=$modifier, " +
            "drivingSide=$drivingSide, " +
            "componentList=$componentList" +
            ")"
    }

    /**
     * @return Builder matching the one used to create this instance
     */
    fun toBuilder(): Builder {
        return Builder()
            .id(id)
            .text(text)
            .type(type)
            .degrees(degrees)
            .modifier(modifier)
            .drivingSide(drivingSide)
            .componentList(componentList)
    }

    /**
     * Build a new [SecondaryManeuver]
     * @property id String
     * @property text String
     * @property type String?
     * @property degrees Double?
     * @property modifier String?
     * @property drivingSide String?
     * @property componentList List<Component>
     */
    class Builder {
        private var id: String = UUID.randomUUID().toString()
        private var text: String = ""
        private var type: String? = null
        private var degrees: Double? = null
        private var modifier: String? = null
        private var drivingSide: String? = null
        private var componentList: List<Component> = listOf()

        /**
         * apply id to the Builder.
         * @param id Long
         * @return Builder
         */
        fun id(id: String): Builder =
            apply { this.id = id }

        /**
         * apply text to the Builder.
         * @param text String
         * @return Builder
         */
        fun text(text: String): Builder =
            apply { this.text = text }

        /**
         * apply type to the Builder.
         * @param type String?
         * @return Builder
         */
        fun type(type: String?): Builder =
            apply { this.type = type }

        /**
         * apply degrees to the Builder.
         * @param degrees Double?
         * @return Builder
         */
        fun degrees(degrees: Double?): Builder =
            apply { this.degrees = degrees }

        /**
         * apply modifier to the Builder.
         * @param modifier String?
         * @return Builder
         */
        fun modifier(modifier: String?): Builder =
            apply { this.modifier = modifier }

        /**
         * apply drivingSide to the Builder.
         * @param drivingSide String?
         * @return Builder
         */
        fun drivingSide(drivingSide: String?): Builder =
            apply { this.drivingSide = drivingSide }

        /**
         * apply componentList to the Builder.
         * @param componentList List<Component>
         * @return Builder
         */
        fun componentList(componentList: List<Component>): Builder =
            apply { this.componentList = componentList }

        /**
         * Build the [SecondaryManeuver]
         * @return SecondaryManeuver
         */
        fun build(): SecondaryManeuver {
            return SecondaryManeuver(
                id,
                text,
                type,
                degrees,
                modifier,
                drivingSide,
                componentList
            )
        }
    }
}
