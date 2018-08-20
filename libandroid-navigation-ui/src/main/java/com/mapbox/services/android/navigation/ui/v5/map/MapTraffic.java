package com.mapbox.services.android.navigation.ui.v5.map;

class MapTraffic {

  private static final String TRAFFIC_LAYER_ID = "traffic";
  private static final String TRAFFIC_TILE_SET_ID = "mapbox.mapbox-traffic-v1";
  private final MapLayerInteractor mapLayerInteractor;

  MapTraffic(MapLayerInteractor mapLayerInteractor) {
    this.mapLayerInteractor = mapLayerInteractor;
  }

  void updateTrafficVisibility(boolean isVisible) {
    mapLayerInteractor.updateLayerVisibility(isVisible, TRAFFIC_TILE_SET_ID, TRAFFIC_LAYER_ID);
  }

  boolean isTrafficVisible() {
    return mapLayerInteractor.isLayerVisible(TRAFFIC_TILE_SET_ID, TRAFFIC_LAYER_ID);
  }
}
