package com.mapbox.navigation.examples.core

import com.mapbox.navigation.base.route.Router

class OnboardRouterActivityKt : BaseRouterActivityKt() {

    override fun setupRouter(): Router = setupOnboardRouter()
}
