package com.mapbox.navigation.dropin.navigationview

import com.mapbox.geojson.Point
import com.mapbox.navigation.dropin.ClickBehavior
import com.mapbox.navigation.dropin.infopanel.InfoPanelBehavior
import com.mapbox.navigation.dropin.maneuver.ManeuverBehavior
import com.mapbox.navigation.ui.speedlimit.model.SpeedInfoValue

internal class NavigationViewBehavior {
    val maneuverBehavior = ManeuverBehavior()
    val infoPanelBehavior = InfoPanelBehavior()
    val mapClickBehavior = ClickBehavior<Point>()
    val speedInfoBehavior = ClickBehavior<SpeedInfoValue?>()
}
