package com.mapbox.navigation.core.internal

import com.mapbox.common.BaseMapboxInitializer

class MapboxNavigationSDKInitializer : BaseMapboxInitializer<MapboxNavigationSDK>() {

    override val initializerClass = MapboxNavigationSDKInitializerImpl::class.java
}
