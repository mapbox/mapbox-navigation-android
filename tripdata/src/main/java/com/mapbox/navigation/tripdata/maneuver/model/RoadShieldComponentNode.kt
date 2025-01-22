package com.mapbox.navigation.tripdata.maneuver.model

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerText
import com.mapbox.api.directions.v5.models.MapboxShield

/**
 * [ComponentNode] of the type [BannerComponents.ICON]
 * @property text String holds [BannerComponents.text] contained inside [BannerComponents]
 * of type [BannerComponents.ICON]
 * @property shieldUrl String holds the [BannerComponents.imageBaseUrl]
 * @property mapboxShield holds info about [BannerComponents.mapboxShield]
 *
 * E.g.
 * For the given [BannerText]
 * "primary": {
 *      "components": [
 *          {
 *              "type": "exit",
 *              "text": "Exit"
 *          },
 *          {
 *              "type": "exit-number",
 *              "text": "23"
 *          }
 *          {
 *              "imageBaseURL": "https://mapbox-navigation-shields...",
 *              "mapbox_shield": {
 *                  "base_url": "https://api.mapbox.com/styles/v1/",
 *                  "name": "us-interstate",
 *                  "text_color": "black",
 *                  "display_ref": "880"
 *              }
 *              "type": "icon",
 *              "text": "I-880"
 *          },
 *          {
 *              "type": "delimiter",
 *              "text": "/"
 *          },
 *          {
 *              "type": "text",
 *              "text": "Nimitz Freeway"
 *          }
 *          ...
 *      ],
 *      "type": "turn",
 *      "modifier": "right",
 *      "text": "Exit 23 I-880 / Nimitz Freeway"
 * }
 */

class RoadShieldComponentNode private constructor(
    val text: String,
    val shieldUrl: String? = null,
    val mapboxShield: MapboxShield? = null,
) : ComponentNode {

    /**
     * @return builder matching the one used to create this instance
     */
    fun toBuilder(): Builder {
        return Builder()
            .text(text)
            .shieldUrl(shieldUrl)
            .mapboxShield(mapboxShield)
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RoadShieldComponentNode

        if (text != other.text) return false
        if (shieldUrl != other.shieldUrl) return false
        if (mapboxShield != other.mapboxShield) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + shieldUrl.hashCode()
        result = 31 * result + mapboxShield.hashCode()
        return result
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun toString(): String {
        return "RoadShieldComponentNode(" +
            "text='$text', " +
            "shieldUrl=$shieldUrl, " +
            "mapboxShield=$mapboxShield" +
            ")"
    }

    /**
     * Build a new [RoadShieldComponentNode]
     * @property text String
     * @property shieldUrl String?
     */
    class Builder {
        private var text: String = ""
        private var shieldUrl: String? = null
        private var mapboxShield: MapboxShield? = null

        /**
         * apply text to the Builder.
         * @param text String
         * @return Builder
         */
        fun text(text: String): Builder =
            apply { this.text = text }

        /**
         * apply text to the Builder.
         * @param shieldUrl String
         * @return Builder
         */
        fun shieldUrl(shieldUrl: String?): Builder =
            apply { this.shieldUrl = shieldUrl }

        /**
         * apply mapboxShield to the Builder.
         * @param mapboxShield String
         * @return Builder
         */
        fun mapboxShield(mapboxShield: MapboxShield?): Builder =
            apply { this.mapboxShield = mapboxShield }

        /**
         * Build the [RoadShieldComponentNode]
         * @return RoadShieldComponentNode
         */
        fun build(): RoadShieldComponentNode {
            return RoadShieldComponentNode(
                text,
                shieldUrl,
                mapboxShield,
            )
        }
    }
}
