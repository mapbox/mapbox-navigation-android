package com.mapbox.navigation.core.internal.utils

import com.mapbox.common.module.provider.ModuleProviderArgument
import com.mapbox.navigation.base.formatter.DistanceFormatter
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigator.RouterInterface

internal fun paramsProvider(moduleParams: ModuleParams): Array<ModuleProviderArgument> =
    when (moduleParams) {
        is ModuleParams.NavigationRouter -> arrayOf(
            ModuleProviderArgument(
                String::class.java,
                moduleParams.accessToken
            ),
            ModuleProviderArgument(
                RouterInterface::class.java,
                moduleParams.nativeRouter
            ),
            ModuleProviderArgument(
                ThreadController::class.java,
                moduleParams.threadController
            ),
        )
        is ModuleParams.NavigationTripNotification -> arrayOf(
            ModuleProviderArgument(
                NavigationOptions::class.java, moduleParams.navigationOptions
            ),
            ModuleProviderArgument(
                DistanceFormatter::class.java,
                MapboxDistanceFormatter(moduleParams.distanceFormatterOptions),
            ),
        )
        is ModuleParams.CommonLogger -> arrayOf()
    }

internal sealed class ModuleParams {
    class NavigationRouter(
        val accessToken: String,
        val nativeRouter: RouterInterface,
        val threadController: ThreadController,
    ) : ModuleParams()

    class NavigationTripNotification(
        val navigationOptions: NavigationOptions,
        val distanceFormatterOptions: DistanceFormatterOptions,
    ) : ModuleParams()

    object CommonLogger : ModuleParams()
}
