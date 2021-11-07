package com.mapbox.navigation.ui.shield.model

import com.mapbox.api.directions.v5.models.ShieldSpriteAttribute
import com.mapbox.navigation.ui.shield.api.MapboxRouteShieldApi

/**
 * A class that allows you to specify the default shield, you would like to render in case
 * [MapboxRouteShieldApi] returns an [RouteShieldError].
 *
 * The default shield is taken in the form of a raw SVG. When supplying your own SVG make sure
 * you adhere to the limitations mentioned [here](https://developer.android.com/studio/write/vector-asset-studio#svg-support)
 * since this SVG would later be converted to a vector drawable to be able to render it on the view.
 *
 * It's recommended to specify the [spriteAttributes] associated with your SVG specially the
 * height, width and placeholder that would be used when converting the SVG to vector drawable.
 * It's also recommended that height and width be same as height and width specified in the raw svg,
 * whereas the x values for placeholder(x1, y1, x2, y2) should match raw svg viewbox x values and
 * the y values for placeholder(x1, y1, x2, y2) should add up to the raw svg viewbox y value.
 *
 * @property shieldSvg shield in the form of an svg
 * @property spriteAttributes attributes associated with the shield
 */
class MapboxRouteShieldOptions private constructor(
    val shieldSvg: String,
    val spriteAttributes: ShieldSpriteAttribute
) {

    /**
     * @return builder matching the one used to create this instance
     */
    fun toBuilder(): Builder = Builder().also {
        it.shieldSvg(shieldSvg)
        it.spriteAttributes(spriteAttributes)
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "MapboxRouteShieldOptions(" +
            "shieldSvg='$shieldSvg', spriteAttributes=$spriteAttributes" +
            ")"
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MapboxRouteShieldOptions

        if (shieldSvg != other.shieldSvg) return false
        if (spriteAttributes != other.spriteAttributes) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = shieldSvg.hashCode()
        result = 31 * result + spriteAttributes.hashCode()
        return result
    }

    /**
     * Build a new [MapboxRouteShieldOptions]
     * @property shieldSvg String builder for svg representation of a shield
     * @property spriteAttributes builder for attributes associated with the shield
     */
    class Builder {

        private var shieldSvg = """
            <svg xmlns="http://www.w3.org/2000/svg" id="rectangle-white-2" width="60" height="42" viewBox="0 0 20 14">
                <g>
                    <path d="M0,0 H20 V14 H0 Z" fill="none"/>
                    <path d="M3,1 H17 C17,1 19,1 19,3 V11 C19,11 19,13 17,13 H3 C3,13 1,13 1,11 V3 C1,3 1,1 3,1" fill="none" stroke="hsl(230, 18%, 13%)" stroke-linejoin="round" stroke-miterlimit="4px" stroke-width="2"/>
                    <path d="M3,1 H17 C17,1 19,1 19,3 V11 C19,11 19,13 17,13 H3 C3,13 1,13 1,11 V3 C1,3 1,1 3,1" fill="hsl(0, 0%, 100%)"/>
                    <path d="M0,4 H20 V10 H0 Z" fill="none" id="mapbox-text-placeholder"/>
                </g>
            </svg>
        """.trimIndent()
        private var spriteAttributes = ShieldSpriteAttribute
            .builder()
            .width(60)
            .height(42)
            .x(552)
            .y(963)
            .pixelRatio(1)
            .placeholder(listOf(0.0, 4.0, 20.0, 10.0))
            .visible(true)
            .build()

        /**
         * apply svg shield to the builder
         * @param shieldSvg String
         * @return Builder
         */
        fun shieldSvg(shieldSvg: String): Builder = apply {
            this.shieldSvg = shieldSvg
        }

        /**
         * apply sprite attributes to the builder
         * @param spriteAttributes ShieldSpriteAttribute
         * @return Builder
         */
        fun spriteAttributes(spriteAttributes: ShieldSpriteAttribute): Builder = apply {
            this.spriteAttributes = spriteAttributes
        }

        /**
         * Build the [MapboxRouteShieldOptions]
         */
        fun build(): MapboxRouteShieldOptions {
            return MapboxRouteShieldOptions(
                shieldSvg = shieldSvg,
                spriteAttributes = spriteAttributes
            )
        }
    }
}
