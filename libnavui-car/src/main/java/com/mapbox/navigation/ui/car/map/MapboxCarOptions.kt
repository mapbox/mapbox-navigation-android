package com.mapbox.navigation.ui.car.map

import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.Style
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import java.util.Locale

/**
 * The top level options for using Mapbox maps and navigation with Android Auto.
 *
 * @param mapInitOptions Used to initialize more advanced map style configurations
 * @param mapDayStyle Assigns a day style for the car map
 * @param mapNightStyle Assigns a day style for the car map, when null [mapDayStyle] is used
 * @param replayEnabled Enables a replay mode with a simulated driver
 */
@ExperimentalMapboxNavigationAPI
class MapboxCarOptions private constructor(
    val mapInitOptions: MapInitOptions,
    val mapDayStyle: String,
    val mapNightStyle: String?,
    val replayEnabled: Boolean
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder(mapInitOptions).apply {
        mapDayStyle(mapDayStyle)
        mapNightStyle(mapNightStyle)
        replayEnabled(replayEnabled)
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MapboxCarOptions

        if (mapInitOptions != other.mapInitOptions) return false
        if (mapDayStyle != other.mapDayStyle) return false
        if (mapNightStyle != other.mapNightStyle) return false
        if (replayEnabled != other.replayEnabled) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = mapInitOptions.hashCode()
        result = 31 * result + mapDayStyle.hashCode()
        result = 31 * result + (mapNightStyle?.hashCode() ?: 0)
        result = 31 * result + replayEnabled.hashCode()
        return result
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun toString(): String {
        return "MapboxCarOptions(mapInitOptions='$mapInitOptions'," +
            " mapDayStyle='$mapDayStyle'," +
            " mapNightStyle=$mapNightStyle," +
            " replayEnabled=$replayEnabled" +
            ")"
    }

    /**
     * Build a new [MapboxCarOptions]
     */
    @ExperimentalMapboxNavigationAPI
    class Builder(
        private var mapInitOptions: MapInitOptions
    ) {
        private var mapDayStyle: String = Style.TRAFFIC_DAY
        private var mapNightStyle: String? = null
        private var directionsLanguage: String = Locale.getDefault().language
        private var replayEnabled: Boolean = false

        /**
         * Allows you to override the MapInitOptions at runtime.
         */
        fun mapInitOptions(mapInitOptions: MapInitOptions): Builder = apply {
            this.mapInitOptions = mapInitOptions
        }

        /**
         * Automatically set style for android auto day mode.
         */
        fun mapDayStyle(mapDayStyle: String): Builder = apply {
            this.mapDayStyle = mapDayStyle
        }

        /**
         * Automatically set style for android auto day mode.
         * If this is not set, the [mapDayStyle] is used.
         */
        fun mapNightStyle(mapNightStyle: String?): Builder = apply {
            this.mapNightStyle = mapNightStyle
        }

        /**
         * This is temporary but required at the moment.
         * https://github.com/mapbox/mapbox-navigation-android/issues/4686
         */
        fun directionsLanguage(directionsLanguage: String): Builder = apply {
            this.directionsLanguage = directionsLanguage
        }

        /**
         * Enables replay mode.
         * This is temporary but required at the moment.
         * https://github.com/mapbox/mapbox-navigation-android/issues/4935
         */
        fun replayEnabled(replayEnabled: Boolean): Builder = apply {
            this.replayEnabled = replayEnabled
        }

        /**
         * Build the [MapboxCarOptions]
         */
        fun build(): MapboxCarOptions {
            return MapboxCarOptions(
                mapInitOptions = mapInitOptions,
                mapDayStyle = mapDayStyle,
                mapNightStyle = mapNightStyle,
                replayEnabled = replayEnabled,
            )
        }
    }
}
