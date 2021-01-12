package com.mapbox.navigation.ui.base.model.maneuver

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerText

/**
 * [ComponentNode] of the type [BannerComponents.ICON]
 * @property text String holds [BannerComponents.text] contained inside [BannerComponents]
 * of type [BannerComponents.ICON]
 * @property shieldIcon ByteArray contains the svg representation of the freeway number given an
 * appropriate url else null.
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
 *
 * text = "I-880"
 * startIndex = 6
 * i.e.
 * components[0].text.length +
 * components[1].text.length
 */

class RoadShieldComponentNode private constructor(
    val text: String,
    val shieldIcon: ByteArray? = null
) : ComponentNode {

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RoadShieldComponentNode

        if (text != other.text) return false
        if (shieldIcon != null) {
            if (other.shieldIcon == null) return false
            if (!shieldIcon.contentEquals(other.shieldIcon)) return false
        } else if (other.shieldIcon != null) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + (shieldIcon?.contentHashCode() ?: 0)
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RoadShieldComponentNode(" +
            "text='$text', " +
            "shieldIcon=${shieldIcon?.contentToString()}" +
            ")"
    }

    /**
     * @return builder matching the one used to create this instance
     */
    fun toBuilder(): Builder {
        return Builder()
            .text(text)
            .shieldIcon(shieldIcon)
    }

    /**
     * Build a new [RoadShieldComponentNode]
     * @property text String
     * @property shieldIcon ByteArray?
     */
    class Builder {
        private var text: String = ""
        private var shieldIcon: ByteArray? = null

        /**
         * apply text to the Builder.
         * @param text String
         * @return Builder
         */
        fun text(text: String): Builder =
            apply { this.text = text }

        /**
         * apply shieldIcon to the Builder.
         * @param shieldIcon ByteArray?
         * @return Builder
         */
        fun shieldIcon(shieldIcon: ByteArray?): Builder =
            apply { this.shieldIcon = shieldIcon }

        /**
         * Build the [RoadShieldComponentNode]
         * @return RoadShieldComponentNode
         */
        fun build(): RoadShieldComponentNode {
            return RoadShieldComponentNode(
                text,
                shieldIcon
            )
        }
    }
}
