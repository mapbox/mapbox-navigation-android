package com.mapbox.services.android.navigation.v5.navigation

import android.content.Context
import com.mapbox.api.routetiles.v1.MapboxRouteTiles
import com.mapbox.core.exceptions.ServicesException
import com.mapbox.geojson.BoundingBox
import okhttp3.ResponseBody
import retrofit2.Callback

/**
 * This is a wrapper class for the [MapboxRouteTiles] class. This class takes care of
 * interfacing with [MapboxRouteTiles] and receives a TAR file wrapped in a ResponseBody
 * which can be handled using a
 * [com.mapbox.services.android.navigation.v5.utils.DownloadTask].
 */
class OfflineTiles private constructor(
    private val mapboxRouteTiles: MapboxRouteTiles,
    private val version: String
) {

    /**
     * Call when you have constructed your OfflineTiles object with your desired parameters.
     * [Callback] must be passed into the method to handle both the response and failure.
     *
     * @param callback for retrofit
     */
    internal fun fetchRouteTiles(callback: Callback<ResponseBody>) {
        mapboxRouteTiles.enqueueCall(callback)
    }

    /**
     * Returns the version of the current builder.
     *
     * @return the version of the current builder
     */
    fun version(): String {
        return version
    }

    /**
     * This builder is used to create a new request to the Mapbox Route Tiles API. A request and
     * therefore a builder must include a version, access token, and a [BoundingBox].
     */
    class Builder internal constructor(context: Context) {
        private var mapboxRouteTilesBuilder: MapboxRouteTiles.Builder = MapboxRouteTiles.builder()
        private lateinit var _version: String

        // internal constructor for tests
        internal constructor(mapboxRouteTilesBuilder: MapboxRouteTiles.Builder, context: Context) : this(context) {
            this.mapboxRouteTilesBuilder = mapboxRouteTilesBuilder
        }

        /**
         * The string version for the tile set being requested. To fetch all available versions, use
         * [OfflineTileVersions].
         *
         * @param version of tiles being requested
         * @return this builder for chaining options together
         */
        fun version(version: String): Builder {
            this._version = version
            mapboxRouteTilesBuilder.version(version)
            return this
        }

        /**
         *
         * Required to call when this is being built. If no access token provided,
         * [ServicesException] will be thrown by the [MapboxRouteTiles.Builder].
         *
         * @param accessToken Mapbox access token, You must have a Mapbox account inorder to use the
         * Optimization API
         * @return this builder for chaining options together
         */
        fun accessToken(accessToken: String): Builder {
            mapboxRouteTilesBuilder.accessToken(accessToken)
            return this
        }

        /**
         * The bounding box representing the region of tiles being requested. The API can handle a
         * maximum of 1.5 million square kilometers.
         *
         * @param boundingBox representing the region
         * @return this builder for chaining options together
         */
        fun boundingBox(boundingBox: BoundingBox): Builder {
            mapboxRouteTilesBuilder.boundingBox(boundingBox)
            return this
        }

        /**
         * Builds a new OfflineTiles object.
         *
         * @return a new instance of OfflineTiles
         */
        fun build(): OfflineTiles {
            check(::_version.isInitialized) {
                "`com.mapbox.services.android.navigation.v5.navigation.OfflineTiles.Builder.version(String)` must be set"
            }
            return OfflineTiles(mapboxRouteTilesBuilder.build(), _version)
        }
    }

    companion object {

        /**
         * Gets a new Builder to build an [OfflineTiles] object
         *
         * @return a new builder
         */
        @JvmStatic
        fun builder(context: Context): Builder = Builder(context)

        @JvmStatic
        // internal constructor for tests
        internal fun builder(mapboxRouteTilesBuilder: MapboxRouteTiles.Builder, context: Context): Builder = Builder(mapboxRouteTilesBuilder, context)
    }
}
