package com.mapbox.navigation.ui.internal.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.mapbox.maps.LayerPosition;
import com.mapbox.maps.Style;
import com.mapbox.maps.extension.style.layers.Layer;

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
  public static void addLayerToMap(@NonNull Style style, @NonNull Layer layer, @Nullable String idBelowLayer) {
    layer.bindTo(style, new LayerPosition(null, idBelowLayer, null));
  }

  /**
   * Generic method for adding a new layer to the map that's above another specific layer.
   *
   * @param style that the current mapView is using
   * @param layer a layer that will be added to the map
   * @param idAboveLayer optionally providing the layer which the new layer should be placed above
   */
  public static void addLayerToMapAbove(@NonNull Style style, @NonNull Layer layer, @Nullable String idAboveLayer) {
    layer.bindTo(style, new LayerPosition(idAboveLayer, null, null));
  }
}

