package com.mapbox.navigation.ui.internal.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.Layer;

/**
 * Utils class useful for performing map operations such as adding sources, layers, and more.
 *
 */
public final class MapUtils {

  private MapUtils() {
    // Hide constructor to prevent initialization
  }

  /**
   * Generic method for adding layers to the map.
   *
   * @param style that the current mapView is using
   * @param layer a layer that will be added to the map
   * @param idBelowLayer optionally providing the layer which the new layer should be placed below
   */
  public static void addLayerToMap(@NonNull Style style, @NonNull Layer layer,
      @Nullable String idBelowLayer) {
    if (layer != null && style.getLayer(layer.getId()) != null) {
      return;
    }
    if (idBelowLayer == null) {
      style.addLayer(layer);
    } else {
      style.addLayerBelow(layer, idBelowLayer);
    }
  }
}
