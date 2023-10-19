package com.mapbox.navigation.core.internal.utils

import com.mapbox.common.module.provider.ModuleProviderArgument
import com.mapbox.navigation.base.formatter.DistanceFormatter
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.internal.trip.notification.TripNotificationInterceptorOwner
import com.mapbox.navigation.base.internal.utils.RoutesParsingQueue
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
            ModuleProviderArgument(
                RoutesParsingQueue::class.java,
                moduleParams.routesParsingQueue
            )
        )
        is ModuleParams.NavigationTripNotification -> arrayOf(
            ModuleProviderArgument(
                NavigationOptions::class.java,
                moduleParams.navigationOptions
            ),
            ModuleProviderArgument(
                TripNotificationInterceptorOwner::class.java,
                moduleParams.tripNotificationInterceptorOwner
            ),
            ModuleProviderArgument(
                DistanceFormatter::class.java,
                MapboxDistanceFormatter(moduleParams.distanceFormatterOptions),
            ),
        )
    }

internal sealed class ModuleParams {
    class NavigationRouter(
        val accessToken: String,
        val nativeRouter: RouterInterface,
        val threadController: ThreadController,
        val routesParsingQueue: RoutesParsingQueue
    ) : ModuleParams()

    class NavigationTripNotification(
        val navigationOptions: NavigationOptions,
        val tripNotificationInterceptorOwner: TripNotificationInterceptorOwner,
        val distanceFormatterOptions: DistanceFormatterOptions,
    ) : ModuleParams()
}
