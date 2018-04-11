package com.mapbox.services.android.navigation.ui.v5.map;

import android.graphics.Bitmap;

import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.style.layers.Layer;

public class MapLayerInteractor {

  private MapboxMap mapboxMap;

  MapLayerInteractor(MapboxMap mapboxMap) {
    this.mapboxMap = mapboxMap;
  }

  public void addLayer(Layer layer) {
    mapboxMap.addLayer(layer);
  }

  public Layer retrieveLayerFromId(String layerId) {
    return mapboxMap.getLayerAs(layerId);
  }

  public void addLayerImage(String imageName, Bitmap image) {
    mapboxMap.addImage(imageName, image);
  }
}
