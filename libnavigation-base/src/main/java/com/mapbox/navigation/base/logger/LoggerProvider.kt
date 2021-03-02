package com.mapbox.navigation.base.logger

import com.mapbox.annotation.module.MapboxModuleType
import com.mapbox.base.common.logger.Logger
import com.mapbox.common.module.provider.MapboxModuleProvider
import com.mapbox.common.module.provider.ModuleProviderArgument

/**
 * Singleton provider of [Logger].
 */
object LoggerProvider {

    val logger = MapboxModuleProvider.createModule<Logger>(MapboxModuleType.CommonLogger, ::paramsProvider)

    private fun paramsProvider(type: MapboxModuleType): Array<ModuleProviderArgument> {
        return arrayOf()
    }
}
