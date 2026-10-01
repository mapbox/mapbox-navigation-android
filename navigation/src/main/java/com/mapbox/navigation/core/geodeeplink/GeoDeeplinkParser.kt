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
 * - Additional query parameters after & are ignored
 *     geo:0,0?q=Restaurants&sourceApplication=exampleApp
 * - RFC 5870 parameters after ; are ignored
 *     geo:37.757527,-122.392937;u=35
 *
 * The scheme is matched case-insensitively. Coordinates outside the valid latitude and longitude
 * ranges are ignored, and a "%" in the query that is not a valid escape is kept as it is.
 */
object GeoDeeplinkParser {

    /**
     * Convert a string into a [GeoDeeplink] object.
     * Returns a [GeoDeeplink] when the string starts with "geo:", ignoring case.
     *
     * [GeoDeeplink.point] or [GeoDeeplink.placeQuery] will provide
     * a supported value or else the [GeoDeeplink] will be null.
     */
    @JvmStatic
    fun parse(geoDeeplink: String?): GeoDeeplink? {
        return if (geoDeeplink != null && geoDeeplink.startsWith(GEO_SCHEME, ignoreCase = true)) {
            val query = geoDeeplink.substring(GEO_SCHEME.length)
            val args = query.split("?")
            val point = args[0].substringBefore(';').toPoint() ?: args.query()?.queryCoordinates()
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
        return if (
            latitude != null && latitude in MIN_LATITUDE..MAX_LATITUDE &&
            longitude != null && longitude in MIN_LONGITUDE..MAX_LONGITUDE &&
            (latitude != 0.0 || longitude != 0.0)
        ) {
            Point.fromLngLat(longitude, latitude)
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
        val decode = safeDecode()
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
        ?.split("&")
        ?.firstOrNull()

    private fun String.removeCoordinates(): String? {
        val decode = safeDecode()
        val withoutAtSign = decode.split("@").firstOrNull()
        val fromInsideParenthesis = decode.split("(", ")").getOrNull(1)
        return fromInsideParenthesis ?: withoutAtSign
    }

    /**
     * [URLDecoder.decode] throws on a "%" that is not followed by two hex digits. Such a "%" is
     * kept as a literal character, and the rest of the query, including valid escapes and "+",
     * is still decoded.
     */
    private fun String.safeDecode(): String {
        val escaped = replace(STRAY_PERCENT, "%25")
        return runCatching { URLDecoder.decode(escaped, "UTF-8") }.getOrDefault(this)
    }

    private const val GEO_SCHEME = "geo:"
    private val STRAY_PERCENT = Regex("%(?![0-9A-Fa-f]{2})")
    private const val MIN_LATITUDE = -90.0
    private const val MAX_LATITUDE = 90.0
    private const val MIN_LONGITUDE = -180.0
    private const val MAX_LONGITUDE = 180.0
}
