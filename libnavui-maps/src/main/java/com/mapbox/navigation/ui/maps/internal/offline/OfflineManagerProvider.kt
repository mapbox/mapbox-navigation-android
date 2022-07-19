package com.mapbox.navigation.ui.maps.internal.offline

import com.mapbox.maps.OfflineManager
import com.mapbox.maps.OfflineManagerInterface
import com.mapbox.maps.ResourceOptions

object OfflineManagerProvider {

    fun provideOfflineManager(
        resourceOptions: ResourceOptions
    ): OfflineManagerInterface = OfflineManager(resourceOptions)
}
