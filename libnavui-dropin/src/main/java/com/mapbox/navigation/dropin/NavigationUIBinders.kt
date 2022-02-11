package com.mapbox.navigation.dropin

import com.mapbox.navigation.dropin.binder.UIBinder
import com.mapbox.navigation.dropin.component.speedlimit.SpeedLimitViewBinder

class NavigationUIBinders(
    val speedLimit: UIBinder = SpeedLimitViewBinder()
)
