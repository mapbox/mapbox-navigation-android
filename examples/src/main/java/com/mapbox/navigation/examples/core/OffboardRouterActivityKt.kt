package com.mapbox.navigation.examples.core

import com.mapbox.navigation.base.route.Router

class OffboardRouterActivityKt : BaseRouterActivityKt() {

    override fun setupRouter(): Router = setupOffboardRouter(this)
}
