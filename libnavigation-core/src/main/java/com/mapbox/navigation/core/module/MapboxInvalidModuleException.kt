package com.mapbox.navigation.core.module

import com.mapbox.annotation.MODULE_CONFIGURATION_CLASS_NAME_FORMAT
import com.mapbox.annotation.MODULE_CONFIGURATION_PROVIDER_VARIABLE_NAME
import com.mapbox.annotation.navigation.module.MapboxNavigationModule
import com.mapbox.annotation.navigation.module.MapboxNavigationModuleType

internal class MapboxInvalidModuleException(type: MapboxNavigationModuleType) : RuntimeException(
    """
    ${type.name} has been excluded from build but a correct alternative was not provided.
    Make sure that:
    - Your custom module implements ${type.interfacePackage}.${type.interfaceClassName}.
    - Your custom module class is annotated with @${MapboxNavigationModule::class.java.simpleName}(${type.name}).
    - You've provided a `ModuleProvider` instance to ${String.format(
        MODULE_CONFIGURATION_CLASS_NAME_FORMAT,
        type.name
    )}#set${MODULE_CONFIGURATION_PROVIDER_VARIABLE_NAME.capitalize()} before initializing the library,
      unless `skipConfiguration` flag is set and your implementation has a public, non-arg constructor.
  """.trimIndent()
)
