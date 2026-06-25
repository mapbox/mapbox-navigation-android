package com.mapbox.navigation.core.internal.utils

import com.mapbox.common.module.provider.ModuleProviderArgument
import com.mapbox.navigation.base.formatter.DistanceFormatter
import com.mapbox.navigation.base.internal.trip.notification.TripNotificationInterceptorOwner
import com.mapbox.navigation.base.options.NavigationOptions

internal fun paramsProvider(moduleParams: ModuleParams): Array<ModuleProviderArgument> =
    when (moduleParams) {
        is ModuleParams.NavigationTripNotification -> arrayOf(
            ModuleProviderArgument(
                NavigationOptions::class.java,
                moduleParams.navigationOptions,
            ),
            ModuleProviderArgument(
                TripNotificationInterceptorOwner::class.java,
                moduleParams.tripNotificationInterceptorOwner,
            ),
            ModuleProviderArgument(
                DistanceFormatter::class.java,
                moduleParams.distanceFormatter,
            ),
        )
    }

internal sealed class ModuleParams {
    class NavigationTripNotification(
        val navigationOptions: NavigationOptions,
        val tripNotificationInterceptorOwner: TripNotificationInterceptorOwner,
        val distanceFormatter: DistanceFormatter,
    ) : ModuleParams()
}
