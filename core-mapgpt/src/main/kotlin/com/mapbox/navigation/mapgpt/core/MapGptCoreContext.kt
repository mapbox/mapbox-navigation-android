package com.mapbox.navigation.mapgpt.core

import com.mapbox.navigation.mapgpt.core.common.Log
import com.mapbox.navigation.mapgpt.core.common.PlatformSettings
import com.mapbox.navigation.mapgpt.core.common.PlatformSettingsFactory
import com.mapbox.navigation.mapgpt.core.common.SharedLog
import com.mapbox.navigation.mapgpt.core.language.LanguageRepository
import com.mapbox.navigation.mapgpt.core.language.LanguageRepositoryImpl
import com.mapbox.navigation.mapgpt.core.reachability.SharedReachability

interface MapGptCoreContext : MiddlewareContext {
    val platformContext: PlatformContext
    val log: Log
    val userSettings: PlatformSettings
    val reachability: SharedReachability
    val languageRepository: LanguageRepository

    interface Builder {
        var log: Log
        var userSettings: PlatformSettings
        var reachability: SharedReachability
        var languageRepository: LanguageRepository

        fun build(): MapGptCoreContext
        fun configure(closure: Builder.() -> Unit): MapGptCoreContext
    }

    companion object {
        fun Builder(platformContext: PlatformContext): Builder {
            return mapGptCoreContextBuilder(platformContext)
        }
    }
}

private fun mapGptCoreContextBuilder(
    platformContext: PlatformContext,
): MapGptCoreContext.Builder {
    return object : MapGptCoreContext.Builder {
        override var log: Log = SharedLog
        override var userSettings: PlatformSettings =
            PlatformSettingsFactory.createPersistentSettings()
        override var reachability: SharedReachability = SharedReachability()
        override var languageRepository: LanguageRepository = LanguageRepositoryImpl()

        override fun build() = MapGptCoreContextImpl(
            platformContext,
            log,
            userSettings,
            reachability,
            languageRepository,
        )

        override fun configure(closure: MapGptCoreContext.Builder.() -> Unit) =
            this.apply(closure).build()
    }
}

private class MapGptCoreContextImpl(
    override val platformContext: PlatformContext,
    override val log: Log,
    override val userSettings: PlatformSettings,
    override val reachability: SharedReachability,
    override val languageRepository: LanguageRepository,
): MapGptCoreContext
