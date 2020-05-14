package com.mapbox.navigation.examples.core

import com.mapbox.navigation.base.options.DeviceProfile
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.examples.utils.Utils

class OnboardRouterActivityKt : BaseRouterActivityKt() {

    override fun setupRouter(deviceProfile: DeviceProfile): Router =
        setupOnboardRouter(Utils.getMapboxAccessToken(this), deviceProfile, applicationContext)
}
