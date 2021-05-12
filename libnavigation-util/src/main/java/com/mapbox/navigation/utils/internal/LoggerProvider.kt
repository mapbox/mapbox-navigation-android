package com.mapbox.navigation.utils.internal

import com.mapbox.annotation.module.MapboxModuleType
import com.mapbox.base.common.logger.Logger
import com.mapbox.common.module.provider.MapboxModuleProvider

/**
 * Singleton provider of [Logger].
 */
object LoggerProvider {

    val logger = MapboxModuleProvider.createModule<Logger>(
        MapboxModuleType.CommonLogger
    ) {
        arrayOf()
    }
}
