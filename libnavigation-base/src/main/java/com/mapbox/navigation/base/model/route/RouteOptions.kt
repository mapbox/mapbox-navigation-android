package com.mapbox.navigation.base.model.route

import com.mapbox.navigation.base.typedef.ProfileCriteria

data class RouteOptions internal constructor(
    private var language: String? = null,
    private var voiceUnits: String? = null,
    private var profile: String? = null
) {

    fun language(): String? = language

    fun voiceUnits(): String? = voiceUnits

    fun profile(): String? = profile

    class Builder {
        var language: String? = null
        var voiceUnits: String? = null
        var profile: String? = null

        fun language(language: String) =
                apply { this.language = language }

        fun voiceUnits(voiceUnits: String) =
                apply { this.voiceUnits = voiceUnits }

        fun profile(@ProfileCriteria profile: String) =
                apply { this.profile = profile }

        fun build(): RouteOptions {
            return RouteOptions(language, voiceUnits, profile)
        }
    }
}
