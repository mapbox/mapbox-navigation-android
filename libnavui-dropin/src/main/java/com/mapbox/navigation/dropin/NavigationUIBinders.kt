package com.mapbox.navigation.dropin

import com.mapbox.navigation.dropin.binder.UIBinder
import com.mapbox.navigation.dropin.binder.infopanel.InfoPanelTripProgressBinder
import com.mapbox.navigation.dropin.component.maneuver.ManeuverViewBinder
import com.mapbox.navigation.dropin.component.roadlabel.RoadNameViewBinder
import com.mapbox.navigation.dropin.component.speedlimit.SpeedLimitViewBinder

class NavigationUIBinders(
    val speedLimit: UIBinder = SpeedLimitViewBinder(),
    val maneuver: UIBinder = ManeuverViewBinder(),
    val roadName: UIBinder = RoadNameViewBinder(),
    val infoPanelTripProgressBinder: UIBinder = InfoPanelTripProgressBinder(),
    val infoPanelHeaderBinder: UIBinder? = null,
    val infoPanelContentBinder: UIBinder? = null
)
