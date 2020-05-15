package com.mapbox.navigation.examples.core

import com.mapbox.navigation.base.options.DeviceProfile
import com.mapbox.navigation.base.route.Router

/**
 * Along with [BaseRouterActivityKt], this activity shows how to
 * use the Navigation SDK's
 * [com.mapbox.navigation.route.offboard.MapboxOffboardRouter].
 */
class OffboardRouterActivityKt : BaseRouterActivityKt() {

    override fun setupRouter(deviceProfile: DeviceProfile): Router = setupOffboardRouter(this)
}
