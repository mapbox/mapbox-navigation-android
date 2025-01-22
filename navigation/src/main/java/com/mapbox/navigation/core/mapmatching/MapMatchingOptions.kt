package com.mapbox.navigation.core.mapmatching

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.DirectionsCriteria.ProfileCriteria
import com.mapbox.api.directions.v5.models.LegAnnotation
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import java.net.URLEncoder

/**
 * Represents options for Mapbox Map Matching API
 * @param coordinates A semicolon-separated list of {longitude},{latitude}
 * coordinate pairs to visit in order.
 * @param waypoints A semicolon-separated list indicating which input coordinates
 * should be treated as waypoints.
 * @param profile A Mapbox Directions routing profile ID, see [DirectionsCriteria.ProfileCriteria]
 * @param user the user parameter of the request
 * @param baseUrl Base url for the request
 * @param radiuses defines radius in meters to snap to the road network.
 * @param timestamps A list of timestamps corresponding to each coordinate.
 * @param annotations list of [MapMatchingAnnotations] to request.
 * @param language defines language for turn-by-turn text instructions.
 * @param bannerInstructions defines whether to return banner instructions
 * associated with the route steps.
 * @param roundaboutExits defines whether to emit instructions at roundabout exits.
 * @param voiceInstructions defines whether to return SSML marked-up text for
 * voice guidance along the route.
 * @param tidy defines whether to remove clusters and re-samples traces
 * for improved map matching results.
 * @param waypointNames will be used for the arrival instruction in banners
 * and voice instructions.
 * @param ignore defines defines certain routing restrictions to ignore when map matching.
 * @param openlrSpec defines the logical format for OpenLR encoded [coordinates].
 * @param openlrFormat defines binary format for OpenLR encoded coordinates.
 */
@ExperimentalPreviewMapboxNavigationAPI
class MapMatchingOptions private constructor(
    val coordinates: String,
    val waypoints: List<Int>?,
    val profile: String,
    val user: String,
    val baseUrl: String,
    val radiuses: List<Double?>?,
    val timestamps: List<Int>?,
    @MapMatchingAnnotations
    val annotations: List<String>?,
    val language: String?,
    val bannerInstructions: Boolean?,
    val roundaboutExits: Boolean?,
    val voiceInstructions: Boolean?,
    val tidy: Boolean?,
    val waypointNames: List<String?>?,
    @MapMatchingRoutingRestriction
    val ignore: List<String>?,
    val openlrSpec: String?,
    val openlrFormat: String?,
) {
    /**
     * Compares if options are the same
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MapMatchingOptions

        if (coordinates != other.coordinates) return false
        if (waypoints != other.waypoints) return false
        if (profile != other.profile) return false
        if (user != other.user) return false
        if (baseUrl != other.baseUrl) return false
        if (radiuses != other.radiuses) return false
        if (timestamps != other.timestamps) return false
        if (annotations != other.annotations) return false
        if (language != other.language) return false
        if (bannerInstructions != other.bannerInstructions) return false
        if (roundaboutExits != other.roundaboutExits) return false
        if (voiceInstructions != other.voiceInstructions) return false
        if (tidy != other.tidy) return false
        if (waypointNames != other.waypointNames) return false
        if (ignore != other.ignore) return false
        if (openlrSpec != other.openlrSpec) return false
        if (openlrFormat != other.openlrFormat) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = coordinates.hashCode()
        result = 31 * result + (waypoints?.hashCode() ?: 0)
        result = 31 * result + profile.hashCode()
        result = 31 * result + user.hashCode()
        result = 31 * result + baseUrl.hashCode()
        result = 31 * result + (radiuses?.hashCode() ?: 0)
        result = 31 * result + (timestamps?.hashCode() ?: 0)
        result = 31 * result + (annotations?.hashCode() ?: 0)
        result = 31 * result + (language?.hashCode() ?: 0)
        result = 31 * result + (bannerInstructions?.hashCode() ?: 0)
        result = 31 * result + (roundaboutExits?.hashCode() ?: 0)
        result = 31 * result + (voiceInstructions?.hashCode() ?: 0)
        result = 31 * result + (tidy?.hashCode() ?: 0)
        result = 31 * result + (waypointNames?.hashCode() ?: 0)
        result = 31 * result + (ignore?.hashCode() ?: 0)
        result = 31 * result + (openlrSpec?.hashCode() ?: 0)
        result = 31 * result + (openlrFormat?.hashCode() ?: 0)
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "MapMatchingOptions(" +
            "coordinates='$coordinates', " +
            "waypoints=$waypoints, " +
            "profile='$profile', " +
            "user='$user', " +
            "baseUrl='$baseUrl', " +
            "radiuses=$radiuses, " +
            "timestamps=$timestamps, " +
            "annotations=$annotations, " +
            "language=$language, " +
            "bannerInstructions=$bannerInstructions, " +
            "roundaboutExits=$roundaboutExits, " +
            "voiceInstructions=$voiceInstructions, " +
            "tidy=$tidy, " +
            "waypointNames=$waypointNames, " +
            "ignore=$ignore, " +
            "openlrSpec=$openlrSpec, " +
            "openlrFormat=$openlrFormat" +
            ")"
    }

    /**
     * Builds a new [MapMatchingOptions]
     */
    class Builder {
        private var coordinates: String = ""
        private var waypoints: List<Int>? = null
        private var baseUrl: String = "https://api.mapbox.com"
        private var user: String = "mapbox"
        private var profile: String = DirectionsCriteria.PROFILE_DRIVING
        private var radiuses: List<Double?>? = null
        private var timestamps: List<Int>? = null
        private var annotations: List<String>? = null
        private var language: String? = null
        private var bannerInstructions: Boolean? = null
        private var roundaboutExits: Boolean? = null
        private var voiceInstructions: Boolean? = null
        private var tidy: Boolean? = null
        private var waypointNames: List<String?>? = null
        private var ignore: List<String>? = null
        private var openlrSpec: String? = null
        private var openlrFormat: String? = null

        /**
         * Required parameter.
         * Replaces [coordinates] previously set in the [Builder].
         * @param coordinates A string which contains
         * semicolon-separated list of {longitude},{latitude}
         * coordinate pairs to visit in order or an OpenLR encoded string.
         * @return this [Builder]
         */
        fun coordinates(coordinates: String): Builder {
            this.coordinates = coordinates
            return this
        }

        /**
         * Required parameter.
         * Replaces [coordinates] previously set in the [Builder].
         * @param coordinates A list of [Point] to visit in order.
         * @return this [Builder]
         */
        fun coordinates(coordinates: List<Point>): Builder {
            this.coordinates = formatCoordinates(coordinates)
            return this
        }

        /**
         * Optional parameter with default value.
         * @param profile a Mapbox Directions routing profile ID
         * @return this [Builder]
         */
        fun profile(@ProfileCriteria profile: String): Builder {
            this.profile = profile
            return this
        }

        /**
         * Optional parameter with default value.
         * @param user the user parameter of the request
         * @return this [Builder]
         */
        fun user(user: String): Builder {
            this.user = user
            return this
        }

        /**
         * Optional parameter.
         * @param waypoints A list of indexes indicating which input coordinates
         * should be treated as waypoints.
         * @return this [Builder]
         */
        fun waypoints(waypoints: List<Int>?): Builder {
            this.waypoints = waypoints
            return this
        }

        /**
         * Optional parameter with default value.
         * @param baseUrl A base url for the request
         * @return this [Builder]
         */
        fun baseUrl(baseUrl: String): Builder {
            this.baseUrl = baseUrl
            return this
        }

        /**
         * Optional parameter.
         * @param radiuses defines radius in meters to snap to the road network.
         * If provided, number of [radiuses] must be the same as number of [coordinates] but you can
         * skip a coordinate using `null` as radius value in the [radiuses].
         * @return this [Builder]
         */
        fun radiuses(radiuses: List<Double?>?): Builder {
            this.radiuses = radiuses
            return this
        }

        /**
         * Optional parameter.
         * @param timestamps A list of timestamps corresponding to each coordinate.
         * If provided number of [timestamps] must be the same as number of [coordinates].
         * @return this [Builder]
         */
        fun timestamps(timestamps: List<Int>?): Builder {
            this.timestamps = timestamps
            return this
        }

        /**
         * Optional parameter.
         * Defines additional metadata to return in [LegAnnotation].
         * @param annotations list of [MapMatchingAnnotations].
         * @return this [Builder]
         */
        fun annotations(@MapMatchingAnnotations annotations: List<String>?): Builder {
            this.annotations = annotations
            return this
        }

        /**
         * Optional parameter.
         * @param language defines language for turn-by-turn text instructions.
         * @return this [Builder]
         */
        fun language(language: String?): Builder {
            this.language = language
            return this
        }

        /**
         * Optional parameter.
         * @param bannerInstructions defines whether to return banner instructions
         * associated with the route steps.
         * @return this [Builder]
         */
        fun bannerInstructions(bannerInstructions: Boolean?): Builder {
            this.bannerInstructions = bannerInstructions
            return this
        }

        /**
         * Optional parameter.
         * @param roundaboutExits defines whether to emit instructions at roundabout exits.
         * @return this [Builder]
         */
        fun roundaboutExits(roundaboutExits: Boolean?): Builder {
            this.roundaboutExits = roundaboutExits
            return this
        }

        /**
         * Optional parameter.
         * @param voiceInstructions defines whether to return SSML marked-up text for
         * voice guidance along the route.
         * @return this [Builder]
         */
        fun voiceInstructions(voiceInstructions: Boolean?): Builder {
            this.voiceInstructions = voiceInstructions
            return this
        }

        /**
         * Optional parameter.
         * @param tidy defines whether to remove clusters and re-samples traces
         * for improved map matching results.
         * @return this [Builder]
         */
        fun tidy(tidy: Boolean?): Builder {
            this.tidy = tidy
            return this
        }

        /**
         * Optional parameter.
         * @param waypointNames will be used for the arrival instruction in banners
         * and voice instructions.
         * If provided number of [waypointNames] must be the same as number of [waypoints],
         * the same as [coordinates] if [waypoints] isn't specified. But you can
         * skip a name using `null` in the [waypointNames].
         * @return this [Builder]
         */
        fun waypointNames(waypointNames: List<String?>?): Builder {
            this.waypointNames = waypointNames
            return this
        }

        /**
         * Optional parameter.
         * @param ignore defines certain routing restrictions to ignore when map matching.
         * @return this [Builder]
         */
        fun ignore(@MapMatchingRoutingRestriction ignore: List<String>?): Builder {
            this.ignore = ignore
            return this
        }

        /**
         * Optional parameter.
         * @param openlrSpec defines the logical format for OpenLR encoded [coordinates].
         * @return this [Builder]
         */
        fun openlrSpec(@MapMatchingOpenLRSpec openlrSpec: String?): Builder {
            this.openlrSpec = openlrSpec
            return this
        }

        /**
         * Optional parameter.
         * @param openlrFormat defines binary format for OpenLR encoded coordinates.
         * @return this [Builder]
         */
        fun openlrFormat(@MapMatchingOpenLRFormat openlrFormat: String?): Builder {
            this.openlrFormat = openlrFormat
            return this
        }

        /**
         * Builds [MapMatchingOptions] based on provided values.
         */
        @Throws(MapMatchingRequiredParameterError::class)
        fun build(): MapMatchingOptions {
            if (coordinates == "") {
                throw MapMatchingRequiredParameterError("coordinates")
            }
            return MapMatchingOptions(
                coordinates = coordinates,
                waypoints = waypoints,
                profile = profile,
                user = user,
                baseUrl = baseUrl,
                radiuses = radiuses,
                timestamps = timestamps,
                annotations = annotations,
                language = language,
                bannerInstructions = bannerInstructions,
                roundaboutExits = roundaboutExits,
                voiceInstructions = voiceInstructions,
                tidy = tidy,
                waypointNames = waypointNames,
                ignore = ignore,
                openlrSpec = openlrSpec,
                openlrFormat = openlrFormat,
            )
        }

        private fun formatCoordinates(coordinates: List<Point>): String =
            coordinates.joinToString(separator = ";") {
                "${it.longitude()},${it.latitude()}"
            }
    }

    internal fun toURL(accessToken: String): String {
        val prefix = "$baseUrl/matching/v5/$user/$profile/" +
            URLEncoder.encode(coordinates, "utf-8") +
            "?access_token=$accessToken" +
            "&steps=true" +
            "&overview=full" +
            "&geometries=${DirectionsCriteria.GEOMETRY_POLYLINE6}"
        return listOf(
            waypoints.toSemicolonSeparatedParameter("waypoints"),
            radiuses.toSemicolonSeparatedParameter("radiuses"),
            timestamps.toSemicolonSeparatedParameter("timestamps"),
            annotations.toCommaSeparatedParameter("annotations"),
            language.toParam("language"),
            bannerInstructions.toParam("banner_instructions"),
            roundaboutExits.toParam("roundabout_exits"),
            voiceInstructions.toParam("voice_instructions"),
            waypointNames.toSemicolonSeparatedParameter("waypoint_names"),
            ignore.toCommaSeparatedParameter("ignore"),
            openlrSpec.toParam("openlr_spec"),
            openlrFormat.toParam("openlr_format"),
            tidy.toParam("tidy"),
        )
            .joinToString(
                prefix = prefix,
                separator = "",
            ) { it.orEmpty() }
    }

    private fun Any?.toParam(name: String) = this?.let {
        "&$name=${URLEncoder.encode(it.toString(), "utf-8")}"
    }

    private fun List<Any?>?.toSemicolonSeparatedParameter(name: String) =
        toSeparatedParameter(name, ";")

    private fun List<Any?>?.toCommaSeparatedParameter(name: String) =
        toSeparatedParameter(name, ",")

    private fun List<Any?>?.toSeparatedParameter(name: String, separator: String): String? {
        return if (this.isNullOrEmpty()) {
            null
        } else {
            this.joinToString(separator = separator, prefix = "&$name=") {
                URLEncoder.encode(it?.toString().orEmpty(), "utf-8")
            }
        }
    }
}

/**
 * Indicates that a required parameter hasn't been set.
 * @param name name of the required parameter which hasn't been set
 */
class MapMatchingRequiredParameterError(val name: String) :
    Throwable("Required parameter $name hasn't been set for Map Matching Request")
