package com.mapbox.navigation.examples.core

import com.mapbox.navigation.base.route.Router

class HybridRouterActivityKt : BaseRouterActivityKt() {
    override fun setupRouter(): Router = setupHybridRouter(this)
}
