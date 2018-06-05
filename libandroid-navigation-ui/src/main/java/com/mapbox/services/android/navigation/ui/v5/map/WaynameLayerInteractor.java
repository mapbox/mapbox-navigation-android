package com.mapbox.services.android.navigation.ui.v5.map;

import android.graphics.Bitmap;

import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.style.layers.Layer;

class WaynameLayerInteractor {

  private MapboxMap mapboxMap;

  WaynameLayerInteractor(MapboxMap mapboxMap) {
    this.mapboxMap = mapboxMap;
  }

  void addLayer(Layer layer) {
    mapboxMap.addLayer(layer);
  }

  Layer retrieveLayerFromId(String layerId) {
    return mapboxMap.getLayerAs(layerId);
  }

  void addLayerImage(String imageName, Bitmap image) {
    mapboxMap.addImage(imageName, image);
  }
}
