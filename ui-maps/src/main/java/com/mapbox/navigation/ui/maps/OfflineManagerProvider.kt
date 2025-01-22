package com.mapbox.navigation.ui.maps

import com.mapbox.maps.OfflineManager

internal object OfflineManagerProvider {

    fun provideOfflineManager(): OfflineManagerProxy = OfflineManagerProxy(OfflineManager())
}
