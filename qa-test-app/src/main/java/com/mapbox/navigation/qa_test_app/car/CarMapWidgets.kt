package com.mapbox.navigation.qa_test_app.car

import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.androidauto.MapboxCarMapObserver
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.maps.extension.androidauto.widgets.CompassWidget
import com.mapbox.maps.extension.androidauto.widgets.LogoWidget

@OptIn(MapboxExperimental::class)
class CarMapWidgets : MapboxCarMapObserver {
  override fun onAttached(mapboxCarMapSurface: MapboxCarMapSurface) {
    super.onAttached(mapboxCarMapSurface)
    with(mapboxCarMapSurface) {
      mapSurface.addWidget(LogoWidget(carContext))
      mapSurface.addWidget(
        CompassWidget(
          carContext,
          marginX = 26f,
          marginY = 120f,
        )
      )
    }
  }
}
