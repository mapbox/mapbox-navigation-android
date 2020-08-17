package com.mapbox.navigation.ui.map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;

import static com.mapbox.navigation.ui.map.MapPaddingCalculator.calculateDefaultPadding;

class MapPaddingAdjustor {

  public static final int BOTTOMSHEET_PADDING_MULTIPLIER_PORTRAIT = 4;
  public static final int BOTTOMSHEET_PADDING_MULTIPLIER_LANDSCAPE = 3;
  public static final int WAYNAME_PADDING_MULTIPLIER = 2;

  private final MapboxMap mapboxMap;
  private final int[] defaultPadding;
  @Nullable
  private int[] customPadding;

  MapPaddingAdjustor(@NonNull MapView mapView, MapboxMap mapboxMap) {
    this.mapboxMap = mapboxMap;
    defaultPadding = calculateDefaultPadding(mapView);
  }

  // Testing only
  MapPaddingAdjustor(MapboxMap mapboxMap, int[] defaultPadding) {
    this.mapboxMap = mapboxMap;
    this.defaultPadding = defaultPadding;
  }

  void updatePaddingWithDefault() {
    customPadding = null;
    updatePaddingWith(defaultPadding);
  }

  void adjustLocationIconWith(@NonNull int[] customPadding) {
    this.customPadding = customPadding;
    updatePaddingWith(customPadding);
  }

  @NonNull
  int[] retrieveCurrentPadding() {
    return mapboxMap.getPadding();
  }

  boolean isUsingDefault() {
    return customPadding == null;
  }

  void updatePaddingWith(int[] padding) {
    mapboxMap.setPadding(padding[0], padding[1], padding[2], padding[3]);
  }

  void resetPadding() {
    if (isUsingDefault()) {
      updatePaddingWithDefault();
    } else {
      adjustLocationIconWith(customPadding);
    }
  }
}
