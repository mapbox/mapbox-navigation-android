package com.mapbox.navigation.examples.core

import com.mapbox.navigation.base.options.DeviceProfile
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.examples.utils.Utils

/**
 * Along with [BaseRouterActivityKt], this activity shows how to
 * use the Navigation SDK's
 * [com.mapbox.navigation.route.hybrid.MapboxHybridRouter].
 */
class HybridRouterActivityKt : BaseRouterActivityKt() {
    override fun setupRouter(deviceProfile: DeviceProfile): Router =
        setupHybridRouter(Utils.getMapboxAccessToken(this), deviceProfile, applicationContext)
}
