package com.mapbox.navigation.tripdata.maneuver.model

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerText

/**
 * [ComponentNode] of the type [BannerComponents.TEXT]
 * @property text String holds [BannerComponents.text] contained inside [BannerComponents]
 * of type [BannerComponents.TEXT]
 * @property abbr String abbreviated form of text.
 * @property abbrPriority Int indicates the order of abbreviation.
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
 * text = "Nimitz Freeway"
 */

class TextComponentNode private constructor(
    val text: String,
    val abbr: String? = null,
    val abbrPriority: Int? = null,
) : ComponentNode {

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TextComponentNode

        if (text != other.text) return false
        if (abbr != other.abbr) return false
        if (abbrPriority != other.abbrPriority) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + abbr.hashCode()
        result = 31 * result + (abbrPriority ?: 0)
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "TextComponentNode(" +
            "text='$text', " +
            "abbr=$abbr, " +
            "abbrPriority=$abbrPriority" +
            ")"
    }

    /**
     * @return builder matching the one used to create this instance
     */
    fun toBuilder(): Builder {
        return Builder()
            .text(text)
            .abbr(abbr)
            .abbrPriority(abbrPriority)
    }

    /**
     * Build a new [TextComponentNode]
     * @property text String
     * @property abbr String?
     * @property abbrPriority Int?
     */
    class Builder {
        private var text: String = ""
        private var abbr: String? = null
        private var abbrPriority: Int? = null

        /**
         * apply text to the Builder.
         * @param text String
         * @return Builder
         */
        fun text(text: String): Builder =
            apply { this.text = text }

        /**
         * apply abbr to the Builder.
         * @param abbr String?
         * @return Builder
         */
        fun abbr(abbr: String?): Builder =
            apply { this.abbr = abbr }

        /**
         * apply abbrPriority to the Builder.
         * @param abbrPriority Int?
         * @return Builder
         */
        fun abbrPriority(abbrPriority: Int?): Builder =
            apply { this.abbrPriority = abbrPriority }

        /**
         * Build the [TextComponentNode]
         * @return TextComponentNode
         */
        fun build(): TextComponentNode {
            return TextComponentNode(
                text,
                abbr,
                abbrPriority,
            )
        }
    }
}
