package com.mapbox.navigation.testing.ui.idling

import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource

abstract class NavigationIdlingResource : IdlingResource {

    fun register() = IdlingRegistry.getInstance().register(this)

    fun unregister() = IdlingRegistry.getInstance().unregister(this)
}
