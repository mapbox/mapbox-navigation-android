package com.mapbox.navigation.ui.base.model.maneuver

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerText

/**
 * [ComponentNode] of the type [BannerComponents.EXIT]
 * @property text String holds [BannerComponents.text] contained inside [BannerComponents]
 * of type [BannerComponents.EXIT]
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
 * text = "Exit"
 */
class ExitComponentNode private constructor(val text: String) : ComponentNode {

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ExitComponentNode

        if (text != other.text) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        return text.hashCode()
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "ExitComponentNode(text='$text')"
    }

    /**
     * @return builder matching the one used to create this instance
     */
    fun toBuilder(): Builder {
        return Builder()
            .text(text)
    }

    /**
     * Build a new [ExitComponentNode]
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
         * Build the [ExitComponentNode]
         * @return ExitComponentNode
         */
        fun build(): ExitComponentNode {
            return ExitComponentNode(
                text
            )
        }
    }
}
