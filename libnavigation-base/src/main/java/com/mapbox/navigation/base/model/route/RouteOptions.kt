package com.mapbox.navigation.base.model.route

import com.mapbox.geojson.Point
import com.mapbox.navigation.base.typedef.ProfileCriteria

data class RouteOptions internal constructor(
    private var baseUrl: String,
    private var user: String,
    private var accessToken: String,
    private var requestUuid: String,
    private var geometries: String,
    private var language: String? = null,
    private var voiceUnits: String? = null,
    private var profile: String? = null,
    private var voiceInstructions: Boolean? = null,
    private var bannerInstructions: Boolean? = null,
    private var coordinates: List<Point>
) {

    fun baseUrl(): String = baseUrl

    fun user(): String = user

    fun accessToken(): String = accessToken

    fun requestUuid(): String = requestUuid

    fun geometries(): String = geometries

    fun language(): String? = language

    fun voiceUnits(): String? = voiceUnits

    fun profile(): String? = profile

    fun voiceInstructions(): Boolean? = voiceInstructions

    fun bannerInstructions(): Boolean? = bannerInstructions

    fun coordinates(): List<Point> = coordinates

    class Builder {
        private lateinit var url: String
        private lateinit var optionsUser: String
        private lateinit var token: String
        private lateinit var reqUuid: String
        private lateinit var geometry: String
        private lateinit var coordinateList: List<Point>
        private var language: String? = null
        private var voiceUnits: String? = null
        private var profile: String? = null
        private var voiceInstructions: Boolean? = null
        private var bannerInstructions: Boolean? = null

        fun baseUrl(baseUrl: String) =
                apply { this.url = baseUrl }

        fun user(user: String) =
                apply { this.optionsUser = user }

        fun accessToken(accessToken: String) =
                apply { this.token = accessToken }

        fun requestUuid(requestUuid: String) =
                apply { this.reqUuid = requestUuid }

        fun geometries(geometries: String) =
                apply { this.geometry = geometries }

        fun language(language: String) =
                apply { this.language = language }

        fun voiceUnits(voiceUnits: String) =
                apply { this.voiceUnits = voiceUnits }

        fun profile(@ProfileCriteria profile: String) =
                apply { this.profile = profile }

        fun voiceInstructions(voiceInstructions: Boolean) =
                apply { this.voiceInstructions = voiceInstructions }

        fun bannerInstructions(bannerInstructions: Boolean) =
                apply { this.bannerInstructions = bannerInstructions }

        fun coordinates(coordinates: List<Point>) =
                apply { this.coordinateList = coordinates }

        fun build(): RouteOptions {
            check(::url.isInitialized) { "Missing property url" }
            check(::optionsUser.isInitialized) { "Missing property url" }
            check(::token.isInitialized) { "Missing property token" }
            check(::reqUuid.isInitialized) { "Missing property reqUuid" }
            check(::geometry.isInitialized) { "Missing property geometry" }
            check(::coordinateList.isInitialized) { "Missing property coordinateList" }
            return RouteOptions(url, optionsUser, token, geometry, reqUuid, language, voiceUnits, profile, voiceInstructions, bannerInstructions, coordinateList)
        }
    }
}
