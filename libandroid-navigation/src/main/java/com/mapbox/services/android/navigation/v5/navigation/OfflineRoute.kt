package com.mapbox.services.android.navigation.v5.navigation

import androidx.annotation.FloatRange
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.core.exceptions.ServicesException
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.utils.extensions.ifNonNull
import com.mapbox.navigator.RouterResult
import okhttp3.HttpUrl

/**
 * The [OfflineRoute] class wraps the [NavigationRoute] class with parameters which
 * could be set in order for an offline navigation session to successfully begin.
 */
class OfflineRoute private constructor(
    private val onlineRoute: NavigationRoute,
    bicycleType: OfflineCriteria.BicycleType?,
    private val cyclingSpeed: Float?,
    private val cyclewayBias: Float?,
    private val hillBias: Float?,
    private val ferryBias: Float?,
    private val roughSurfaceBias: Float?,
    waypointTypes: List<OfflineCriteria.WaypointType?>?
) {
    private val bicycleType: String?
    private val waypointTypes: String?

    init {
        this.bicycleType = bicycleType?.type
        this.waypointTypes = checkWaypointTypes(waypointTypes)
    }

    companion object {

        private const val BICYCLE_TYPE_QUERY_PARAMETER = "bicycle_type"
        private const val CYCLING_SPEED_QUERY_PARAMETER = "cycling_speed"
        private const val CYCLEWAY_BIAS_QUERY_PARAMETER = "cycleway_bias"
        private const val HILL_BIAS_QUERY_PARAMETER = "hill_bias"
        private const val FERRY_BIAS_QUERY_PARAMETER = "ferry_bias"
        private const val ROUGH_SURFACE_BIAS_QUERY_PARAMETER = "rough_surface_bias"
        private const val WAYPOINT_TYPES_QUERY_PARAMETER = "waypoint_types"

        /**
         * Build a new [OfflineRoute] object with the proper offline navigation parameters already setup.
         *
         * @return a [Builder] object for creating this object
         */
        @JvmStatic
        fun builder(onlineRouteBuilder: NavigationRoute.Builder): Builder {
            return Builder(onlineRouteBuilder)
        }
    }

    /**
     * Builds a URL string for offline.
     *
     * @return the offline url string
     */
    fun buildUrl(): String {
        val onlineUrl = onlineRoute.call.request().url().toString()
        return buildOfflineUrl(onlineUrl)
    }

    internal fun retrieveOfflineRoute(response: RouterResult): DirectionsRoute? {
        return if (!response.success) {
            null
        } else {
            obtainRouteFor(response.json)
        }
    }

    private fun checkWaypointTypes(waypointTypes: List<OfflineCriteria.WaypointType?>?): String? {
        return if (waypointTypes.isNullOrEmpty()) {
            null
        } else {
            formatWaypointTypes(waypointTypes)
                ?: throw ServicesException("All waypoint types values must be one of break, through or null")
        }
    }

    private fun formatWaypointTypes(waypointTypesToFormat: List<OfflineCriteria.WaypointType?>): String? {
        val waypointTypes = waypointTypesToFormat.map { it?.type ?: "" }.toTypedArray()
        return waypointTypes.joinTo(StringBuilder(), ";").toString()
    }

    private fun buildOfflineUrl(onlineUrl: String): String {
        val offlineUrlBuilder = HttpUrl.get(onlineUrl).newBuilder()
        ifNonNull(bicycleType) {
            offlineUrlBuilder.addQueryParameter(BICYCLE_TYPE_QUERY_PARAMETER, it)
        }

        ifNonNull(cyclingSpeed) {
            offlineUrlBuilder.addQueryParameter(CYCLING_SPEED_QUERY_PARAMETER, it.toString())
        }

        ifNonNull(cyclewayBias) {
            offlineUrlBuilder.addQueryParameter(CYCLEWAY_BIAS_QUERY_PARAMETER, it.toString())
        }

        ifNonNull(hillBias) {
            offlineUrlBuilder.addQueryParameter(HILL_BIAS_QUERY_PARAMETER, it.toString())
        }

        ifNonNull(ferryBias) {
            offlineUrlBuilder.addQueryParameter(FERRY_BIAS_QUERY_PARAMETER, it.toString())
        }

        ifNonNull(roughSurfaceBias) {
            offlineUrlBuilder.addQueryParameter(ROUGH_SURFACE_BIAS_QUERY_PARAMETER, it.toString())
        }

        ifNonNull(waypointTypes) {
            offlineUrlBuilder.addQueryParameter(WAYPOINT_TYPES_QUERY_PARAMETER, it)
        }
        return offlineUrlBuilder.build().toString()
    }

    private fun obtainRouteFor(response: String): DirectionsRoute? =
        DirectionsResponse.fromJson(response).routes().firstOrNull()

    class Builder internal constructor(private val navigationRouteBuilder: NavigationRoute.Builder) {
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
            navigationRouteBuilder.build(),
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
