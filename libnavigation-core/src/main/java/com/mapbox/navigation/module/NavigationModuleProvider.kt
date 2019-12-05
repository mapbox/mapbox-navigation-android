package com.mapbox.navigation.module

import com.mapbox.annotation.MODULE_CONFIGURATION_CLASS_NAME_FORMAT
import com.mapbox.annotation.MODULE_CONFIGURATION_PROVIDER_METHOD_FORMAT
import com.mapbox.annotation.MODULE_CONFIGURATION_PROVIDER_VARIABLE_NAME
import com.mapbox.annotation.MODULE_CONFIGURATION_SKIPPED_CLASS
import com.mapbox.annotation.MODULE_CONFIGURATION_SKIPPED_PACKAGE
import com.mapbox.annotation.MODULE_CONFIGURATION_SKIP_VARIABLE
import com.mapbox.annotation.MODULE_PROVIDER_PACKAGE_NAVIGATION
import com.mapbox.annotation.navigation.module.MapboxNavigationModuleType

internal object NavigationModuleProvider {

    inline fun <reified T> createModule(
        type: MapboxNavigationModuleType,
        // finding a constructor requires exact params types, not subclass/implementations,
        // that's why we need to pass the expected interface class as well
        paramsProvider: (MapboxNavigationModuleType) -> Array<Pair<Class<*>?, Any?>>
    ): T {
        try {
            val configurationClass = Class.forName(
                "$MODULE_PROVIDER_PACKAGE_NAVIGATION.${String.format(MODULE_CONFIGURATION_CLASS_NAME_FORMAT, type.name)}"
            )

            val skipConfiguration =
                configurationClass.getMethod(MODULE_CONFIGURATION_SKIP_VARIABLE.asGetter())
                    .invoke(null) as Boolean

            val instance: Any

            if (skipConfiguration) {
                val implPackage =
                    configurationClass.getMethod(MODULE_CONFIGURATION_SKIPPED_PACKAGE.asGetter())
                        .invoke(null) as String
                val implClassName =
                    configurationClass.getMethod(MODULE_CONFIGURATION_SKIPPED_CLASS.asGetter())
                        .invoke(null) as String
                val implClass = Class.forName("$implPackage.$implClassName")
                instance = try {
                    // try to invoke a no-arg, public constructor
                    val constructor = implClass.getConstructor()
                    constructor.newInstance()
                } catch (ex: NoSuchMethodException) {
                    // try find default arguments for Mapbox default module
                    val params = paramsProvider.invoke(type)
                    try {
                        val constructor = implClass.getConstructor(*params.map { it.first }.toTypedArray())
                        constructor.newInstance(*params.map { it.second }.toTypedArray())
                    } catch (ex: NoSuchMethodException) {
                        // try to create instance of Kotlin object
                        try {
                            implClass.getField("INSTANCE").get(null)
                        } catch (ex: NoSuchMethodException) {
                            // try to get instance of singleton
                            try {
                                implClass.getMethod("getInstance").invoke(null)
                            } catch (ex: NoSuchMethodException) {
                                throw MapboxInvalidModuleException(type)
                            }
                        }
                    }
                }
            } else {
                val providerField = configurationClass.getDeclaredField(MODULE_CONFIGURATION_PROVIDER_VARIABLE_NAME)
                providerField.isAccessible = true
                val provider = providerField.get(null)

                if (provider != null) {
                    // get module instance from the provider
                    val providerClass = providerField.type
                    val providerMethod = providerClass.getDeclaredMethod(String.format(MODULE_CONFIGURATION_PROVIDER_METHOD_FORMAT, type.name))
                    instance = providerMethod.invoke(provider)
                } else {
                    throw MapboxInvalidModuleException(type)
                }
            }

            if (instance is T) {
                return instance
            } else {
                throw MapboxInvalidModuleException(type)
            }
        } catch (ex: Exception) {
            throw if (ex is MapboxInvalidModuleException) {
                ex
            } else {
                ex.printStackTrace()
                MapboxInvalidModuleException(type)
            }
        }
    }

    private fun String.asGetter() = "get${this[0].toUpperCase()}${this.substring(1)}"
}
