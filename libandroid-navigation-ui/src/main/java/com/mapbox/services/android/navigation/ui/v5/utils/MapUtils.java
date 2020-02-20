package com.mapbox.services.android.navigation.ui.v5.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.Layer;

/**
 * Utils class useful for performing map operations such as adding sources, layers, and more.
 *
 * @since 0.8.0
 */
public final class MapUtils {

  private MapUtils() {
    // Hide constructor to prevent initialization
  }

  /**
   * Generic method for adding layers to the map.
   *
   * @param mapboxMap    that the current mapView is using
   * @param layer        a layer that will be added to the map
   * @param idBelowLayer optionally providing the layer which the new layer should be placed below
   * @since 0.8.0
   * @deprecated use {@link #addLayerToMap(Style, Layer, String)}
   */
  @Deprecated
  public static void addLayerToMap(@NonNull MapboxMap mapboxMap, @NonNull Layer layer,
                                   @Nullable String idBelowLayer) {
    if (layer != null && mapboxMap.getStyle().getLayer(layer.getId()) != null) {
      return;
    }
    if (idBelowLayer == null) {
      mapboxMap.getStyle().addLayer(layer);
    } else {
      mapboxMap.getStyle().addLayerBelow(layer, idBelowLayer);
    }
  }

  /**
   * Generic method for adding layers to the map.
   *
   * @param style        that the current mapView is using
   * @param layer        a layer that will be added to the map
   * @param idBelowLayer optionally providing the layer which the new layer should be placed below
   * @since 0.8.0
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
