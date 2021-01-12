package com.mapbox.navigation.ui.base.model.maneuver

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerText

/**
 * [ComponentNode] of the type [BannerComponents.DELIMITER]
 * @property text String holds [BannerComponents.text] contained inside [BannerComponents]
 * of type [BannerComponents.DELIMITER]
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
 * text = "/"
 */
class DelimiterComponentNode private constructor(val text: String) : ComponentNode {

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DelimiterComponentNode

        if (text != other.text) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = text.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "DelimiterComponentNode(text='$text')"
    }

    /**
     * @return builder matching the one used to create this instance
     */
    fun toBuilder(): Builder {
        return Builder()
            .text(text)
    }

    /**
     * Build a new [DelimiterComponentNode]
     * @property text String
     */
    class Builder {
        private var text: String = ""

        /**
         * apply text to the Builder.
         * @param text String
         * @return Builder
         */
        fun text(text: String): Builder =
            apply { this.text = text }

        /**
         * Build the [DelimiterComponentNode]
         * @return DelimiterComponentNode
         */
        fun build(): DelimiterComponentNode {
            return DelimiterComponentNode(
                text
            )
        }
    }
}
