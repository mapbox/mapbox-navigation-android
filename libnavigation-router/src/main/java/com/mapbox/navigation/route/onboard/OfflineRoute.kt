package com.mapbox.navigation.route.onboard

import android.net.Uri
import androidx.annotation.FloatRange
import com.mapbox.navigation.utils.internal.ifNonNull
import java.net.URL

/**
 * The [OfflineRoute] class wraps the [routeUrl] with parameters which
 * could be set in order for an offline navigation session to successfully begin.
 */
internal class OfflineRoute private constructor(
    private val routeUrl: URL,
    private val bicycleType: OfflineCriteria.BicycleType?,
    private val cyclingSpeed: Float?,
    private val cyclewayBias: Float?,
    private val hillBias: Float?,
    private val ferryBias: Float?,
    private val roughSurfaceBias: Float?,
    private val waypointTypes: List<OfflineCriteria.WaypointType?>?
) {

    private companion object {
        private const val BICYCLE_TYPE_QUERY_PARAMETER = "bicycle_type"
        private const val CYCLING_SPEED_QUERY_PARAMETER = "cycling_speed"
        private const val CYCLEWAY_BIAS_QUERY_PARAMETER = "cycleway_bias"
        private const val HILL_BIAS_QUERY_PARAMETER = "hill_bias"
        private const val FERRY_BIAS_QUERY_PARAMETER = "ferry_bias"
        private const val ROUGH_SURFACE_BIAS_QUERY_PARAMETER = "rough_surface_bias"
        private const val WAYPOINT_TYPES_QUERY_PARAMETER = "waypoint_types"
    }

    /**
     * @return builder matching the one used to create this instance
     */
    fun toBuilder() = Builder(routeUrl)
        .bicycleType(bicycleType)
        .cyclingSpeed(cyclingSpeed)
        .cyclewayBias(cyclewayBias)
        .hillBias(hillBias)
        .ferryBias(ferryBias)
        .roughSurfaceBias(roughSurfaceBias)
        .waypointTypes(waypointTypes)

    /**
     * Builds a URL string for offline.
     *
     * @return the offline url string
     */
    fun buildUrl(): String {
        return buildOfflineUrl(routeUrl)
    }

    private fun checkWaypointTypes(waypointTypes: List<OfflineCriteria.WaypointType?>?): String? {
        return if (waypointTypes.isNullOrEmpty()) {
            null
        } else {
            formatWaypointTypes(waypointTypes)
        }
    }

    private fun formatWaypointTypes(
        waypointTypesToFormat: List<OfflineCriteria.WaypointType?>
    ): String {
        val waypointTypes = waypointTypesToFormat.map { it?.type ?: "" }.toTypedArray()
        return waypointTypes.joinTo(StringBuilder(), ";").toString()
    }

    private fun buildOfflineUrl(url: URL): String {
        val offlineUrlBuilder = Uri.parse(url.toString()).buildUpon()

        offlineUrlBuilder
            .appendQueryParamIfNonNull(BICYCLE_TYPE_QUERY_PARAMETER, bicycleType?.type)
        offlineUrlBuilder.appendQueryParamIfNonNull(CYCLING_SPEED_QUERY_PARAMETER, cyclingSpeed)
        offlineUrlBuilder.appendQueryParamIfNonNull(CYCLEWAY_BIAS_QUERY_PARAMETER, cyclewayBias)
        offlineUrlBuilder.appendQueryParamIfNonNull(HILL_BIAS_QUERY_PARAMETER, hillBias)
        offlineUrlBuilder.appendQueryParamIfNonNull(FERRY_BIAS_QUERY_PARAMETER, ferryBias)
        offlineUrlBuilder.appendQueryParamIfNonNull(
            ROUGH_SURFACE_BIAS_QUERY_PARAMETER,
            roughSurfaceBias
        )
        offlineUrlBuilder.appendQueryParamIfNonNull(
            WAYPOINT_TYPES_QUERY_PARAMETER,
            checkWaypointTypes(waypointTypes)
        )

        return offlineUrlBuilder.build().toString()
    }

    private fun Uri.Builder.appendQueryParamIfNonNull(key: String, value: Any?): Uri.Builder =
        apply {
            ifNonNull(value) {
                appendQueryParameter(key, it.toString())
            }
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OfflineRoute

        if (routeUrl != other.routeUrl) return false
        if (bicycleType != other.bicycleType) return false
        if (cyclingSpeed != other.cyclingSpeed) return false
        if (cyclewayBias != other.cyclewayBias) return false
        if (hillBias != other.hillBias) return false
        if (ferryBias != other.ferryBias) return false
        if (roughSurfaceBias != other.roughSurfaceBias) return false
        if (waypointTypes != other.waypointTypes) return false

        return true
    }

    override fun hashCode(): Int {
        var result = routeUrl.hashCode()
        result = 31 * result + (bicycleType?.hashCode() ?: 0)
        result = 31 * result + (cyclingSpeed?.hashCode() ?: 0)
        result = 31 * result + (cyclewayBias?.hashCode() ?: 0)
        result = 31 * result + (hillBias?.hashCode() ?: 0)
        result = 31 * result + (ferryBias?.hashCode() ?: 0)
        result = 31 * result + (roughSurfaceBias?.hashCode() ?: 0)
        result = 31 * result + (waypointTypes?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "OfflineRoute(" +
            "routeUrl=$routeUrl, " +
            "bicycleType=$bicycleType, " +
            "cyclingSpeed=$cyclingSpeed, " +
            "cyclewayBias=$cyclewayBias, " +
            "hillBias=$hillBias, " +
            "ferryBias=$ferryBias, " +
            "roughSurfaceBias=$roughSurfaceBias, " +
            "waypointTypes=$waypointTypes" +
            ")"
    }

    class Builder internal constructor(private val routeUrl: URL) {
        private var bicycleType: OfflineCriteria.BicycleType? = null
        private var cyclingSpeed: Float? = null
        private var cyclewayBias: Float? = null
        private var hillBias: Float? = null
        private var ferryBias: Float? = null
        private var roughSurfaceBias: Float? = null
        private var waypointTypes: List<OfflineCriteria.WaypointType?>? = null

        /**
         * The type of bicycle, either <tt>Road</tt>, <tt>Hybrid</tt>, <tt>City</tt>, <tt>Cross</tt>, <tt>Mountain</tt>.
         * The default type is <tt>Hybrid</tt>.
         *
         * @param bicycleType the type of bicycle
         * @return this builder for chaining options together
         */
        fun bicycleType(bicycleType: OfflineCriteria.BicycleType?): Builder {
            this.bicycleType = bicycleType
            return this
        }

        /**
         * Cycling speed is the average travel speed along smooth, flat roads. This is meant to be the
         * speed a rider can comfortably maintain over the desired distance of the route. It can be
         * modified (in the costing method) by surface type in conjunction with bicycle type and
         * (coming soon) by hilliness of the road section. When no speed is specifically provided, the
         * default speed is determined by the bicycle type and are as follows: Road = 25 KPH (15.5 MPH),
         * Cross = 20 KPH (13 MPH), Hybrid/City = 18 KPH (11.5 MPH), and Mountain = 16 KPH (10 MPH).
         *
         * @param cyclingSpeed in kmh
         * @return this builder for chaining options together
         */
        fun cyclingSpeed(@FloatRange(from = 5.0, to = 60.0) cyclingSpeed: Float?): Builder {
            this.cyclingSpeed = cyclingSpeed
            return this
        }

        /**
         * A cyclist's propensity to use roads alongside other vehicles. This is a range of values from -1
         * to 1, where -1 attempts to avoid roads and stay on cycleways and paths, and 1 indicates the
         * rider is more comfortable riding on roads. Based on the use_roads factor, roads with certain
         * classifications and higher speeds are penalized in an attempt to avoid them when finding the
         * best path. The default value is 0.
         *
         * @param cyclewayBias a cyclist's propensity to use roads alongside other vehicles
         * @return this builder for chaining options together
         */
        fun cyclewayBias(@FloatRange(from = -1.0, to = 1.0) cyclewayBias: Float?): Builder {
            this.cyclewayBias = cyclewayBias
            return this
        }

        /**
         * A cyclist's desire to tackle hills in their routes. This is a range of values from -1 to 1,
         * where -1 attempts to avoid hills and steep grades even if it means a longer (time and
         * distance) path, while 1 indicates the rider does not fear hills and steeper grades. Based on
         * the hill bias factor, penalties are applied to roads based on elevation change and grade.
         * These penalties help the path avoid hilly roads in favor of flatter roads or less steep
         * grades where available. Note that it is not always possible to find alternate paths to avoid
         * hills (for example when route locations are in mountainous areas). The default value is 0.
         *
         * @param hillBias a cyclist's desire to tackle hills in their routes
         * @return this builder for chaining options together
         */
        fun hillBias(@FloatRange(from = -1.0, to = 1.0) hillBias: Float?): Builder {
            this.hillBias = hillBias
            return this
        }

        /**
         * This value indicates the willingness to take ferries. This is a range of values between -1 and 1.
         * Values near -1 attempt to avoid ferries and values near 1 will favor ferries. Note that
         * sometimes ferries are required to complete a route so values of -1 are not guaranteed to avoid
         * ferries entirely. The default value is 0.
         *
         * @param ferryBias the willingness to take ferries
         * @return this builder for chaining options together
         */
        fun ferryBias(@FloatRange(from = -1.0, to = 1.0) ferryBias: Float?): Builder {
            this.ferryBias = ferryBias
            return this
        }

        /**
         * This value is meant to represent how much a cyclist wants to favor or avoid roads with poor/rough
         * surfaces relative to the bicycle type being used. This is a range of values between -1 and 1.
         * When the value approaches -1, we attempt to penalize heavier or avoid roads with rough surface types
         * so that they are only taken if they significantly improve travel time; only bicycle
         * speed on each surface is taken into account. As the value approaches 1, we will favor rough surfaces.
         * When the value is equal to -1, all bad surfaces are completely disallowed from routing,
         * including start and end points. The default value is 0.
         *
         * @param roughSurfaceBias how much a cyclist wants to avoid roads with poor surfaces
         * @return this builder for chaining options together
         */
        fun roughSurfaceBias(@FloatRange(from = -1.0, to = 1.0) roughSurfaceBias: Float?): Builder {
            this.roughSurfaceBias = roughSurfaceBias
            return this
        }

        /**
         * The same waypoint types the user originally made when the request was made.
         *
         * @param waypointTypes break, through or omitted null
         * @return this builder for chaining options together
         */
        fun waypointTypes(waypointTypes: List<OfflineCriteria.WaypointType?>?): Builder {
            this.waypointTypes = waypointTypes
            return this
        }

        /**
         * This uses the provided parameters set using the [Builder] and adds the required
         * settings for offline navigation to work correctly.
         *
         * @return a new instance of [OfflineRoute]
         */
        fun build(): OfflineRoute = OfflineRoute(
            routeUrl,
            bicycleType,
            cyclingSpeed,
            cyclewayBias,
            hillBias,
            ferryBias,
            roughSurfaceBias,
            waypointTypes
        )
    }
}
