package com.mapbox.navigation.qa_test_app.car

import androidx.car.app.CarContext
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.Style
import com.mapbox.maps.extension.androidauto.MapboxCarMapObserver
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.maps.extension.style.layers.generated.skyLayer
import com.mapbox.maps.extension.style.layers.properties.generated.SkyType
import com.mapbox.maps.extension.style.sources.generated.rasterDemSource
import com.mapbox.maps.extension.style.style
import com.mapbox.maps.extension.style.terrain.generated.terrain

/**
 * Example showing how you can add a sky layer that has a sun direction,
 * and adding a terrain layer to show mountains.
 */
@OptIn(MapboxExperimental::class)
class CarMapShowcase : MapboxCarMapObserver {

  private var mapboxCarMapSurface: MapboxCarMapSurface? = null

  override fun onAttached(mapboxCarMapSurface: MapboxCarMapSurface) {
    this.mapboxCarMapSurface = mapboxCarMapSurface
    loadMapStyle(mapboxCarMapSurface.carContext)
  }

  override fun onDetached(mapboxCarMapSurface: MapboxCarMapSurface) {
    this.mapboxCarMapSurface = null
  }

  fun mapStyleUri(carContext: CarContext): String {
    return if (carContext.isDarkMode) Style.TRAFFIC_NIGHT else Style.TRAFFIC_DAY
  }

  fun loadMapStyle(carContext: CarContext) {
    // https://docs.mapbox.com/mapbox-gl-js/style-spec/layers/#paint-sky-sky-atmosphere-sun
    // Position of the sun center [a azimuthal angle, p polar angle].
    // Azimuthal is degrees from north, where 0.0 is north.
    // Polar is degrees from overhead, where 0.0 is overhead.
    val sunDirection = if (carContext.isDarkMode) listOf(-50.0, 90.2) else listOf(0.0, 0.0)

    mapboxCarMapSurface?.mapSurface?.getMapboxMap()?.loadStyle(
      styleExtension = style(mapStyleUri(carContext)) {
        +rasterDemSource(DEM_SOURCE) {
          url(TERRAIN_URL_TILE_RESOURCE)
          tileSize(514)
        }
        +terrain(DEM_SOURCE)
        +skyLayer(SKY_LAYER) {
          skyType(SkyType.ATMOSPHERE)
          skyAtmosphereSun(sunDirection)
        }
      }
    )
  }

  companion object {
    private const val SKY_LAYER = "sky"
    private const val DEM_SOURCE = "mapbox-dem"
    private const val TERRAIN_URL_TILE_RESOURCE = "mapbox://mapbox.mapbox-terrain-dem-v1"
  }
}
