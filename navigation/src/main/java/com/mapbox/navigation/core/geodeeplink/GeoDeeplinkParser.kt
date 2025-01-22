package com.mapbox.navigation.core.geodeeplink

import com.mapbox.geojson.Point
import java.net.URLDecoder

/**
 * Converts external geo intents into [GeoDeeplink] objects.
 *
 * Public documentation for the geo deeplink can be found here
 * https://developers.google.com/maps/documentation/urls/android-intents
 *
 * Variations supported.
 *
 * - Coordinates without a place query
 *     geo:37.757527,-122.392937
 * - Coordinates with a place query
 *     geo:37.788151,-122.407543?q=3107 Washington Street, San Francisco, California 94115, United States
 * - Place query without coordinates
 *     geo:0,0?q=3107 Washington Street, San Francisco, California 94115, United States
 * - Encoded deep-links
 *     geo:0,0?q=%E5%93%81%E5%B7%9D%E5%8C%BA%E5%A4%A7%E4%BA%95%206-16-16%20%E3%83%A1%E3%82%BE%E3%83%B3%E9%B9%BF%E5%B3%B6%E3%81%AE%E7%A2%A7201%4035.595404%2C139.731737
 * - AT(@) symbol specifying coordinates
 *     geo:0,0?q=Coffee Shop@37.757527,-122.392937
 * - Parenthesis specifying the place query
 *     geo:0,0?q=54.356152,18.642736(ul. 3 maja 12, 80-802 Gdansk, Poland)
 */
object GeoDeeplinkParser {

    /**
     * Convert a string into a [GeoDeeplink] object.
     * Returns a [GeoDeeplink] when the string starts with "geo:"
     *
     * [GeoDeeplink.point] or [GeoDeeplink.placeQuery] will provide
     * a supported value or else the [GeoDeeplink] will be null.
     */
    @JvmStatic
    fun parse(geoDeeplink: String?): GeoDeeplink? {
        return if (geoDeeplink != null && geoDeeplink.startsWith("geo:")) {
            val query = geoDeeplink.substring("geo:".length)
            val args = query.split("?")
            val point = args[0].toPoint() ?: args.query()?.queryCoordinates()
            val placeQuery = args.query()?.removeCoordinates()
            return if (point == null && placeQuery.isNullOrEmpty()) {
                null
            } else {
                GeoDeeplink(
                    point = point,
                    placeQuery = placeQuery,
                )
            }
        } else {
            null
        }
    }

    private fun String.toPoint(): Point? {
        val coordinates = this.split(",")
        if (coordinates.size < 2) return null
        val latitude = coordinates[0].toCoordinate()
        val longitude = coordinates[1].toCoordinate()
            ?: coordinates[1].replace("%20", "").toCoordinate()
        return if (latitude != null && longitude != null && (latitude != 0.0 || longitude != 0.0)) {
            Point.fromLngLat(longitude.toDouble(), latitude.toDouble())
        } else {
            null
        }
    }

    private fun String.toCoordinate(): Double? {
        val coordinate = this.toDoubleOrNull()
        return if (coordinate != null && coordinate.isFinite()) {
            coordinate
        } else {
            null
        }
    }

    private fun String.queryCoordinates(): Point? {
        val decode = URLDecoder.decode(this, "UTF-8")
        return decode.decodeAtSign() ?: decode.decodeParenthesis()
    }

    private fun String.decodeAtSign(): Point? = split("@")
        .lastOrNull()
        ?.toPoint()

    private fun String.decodeParenthesis(): Point? = split("(", ")")
        .firstOrNull()
        ?.toPoint()

    private fun List<String>.query(): String? = firstOrNull { it.startsWith("q=") }
        ?.substring("q=".length)

    private fun String.removeCoordinates(): String? {
        val decode = URLDecoder.decode(this, "UTF-8")
        val withoutAtSign = decode.split("@").firstOrNull()
        val fromInsideParenthesis = decode?.split("(", ")")?.getOrNull(1)
        return fromInsideParenthesis ?: withoutAtSign
    }
}
