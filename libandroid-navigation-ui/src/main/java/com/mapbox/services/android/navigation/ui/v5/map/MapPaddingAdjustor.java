package com.mapbox.services.android.navigation.ui.v5.map;

import android.content.Context;
import android.content.res.Resources;

import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.services.android.navigation.ui.v5.R;

class MapPaddingAdjustor {

  private static final int[] ZERO_MAP_PADDING = {0, 0, 0, 0};
  private static final int BOTTOMSHEET_PADDING_MULTIPLIER = 4;
  private static final int WAYNAME_PADDING_MULTIPLIER = 2;

  private final int defaultTopPadding;
  private final int waynameTopPadding;
  private MapboxMap mapboxMap;

  MapPaddingAdjustor(MapView mapView, MapboxMap mapboxMap) {
    this.mapboxMap = mapboxMap;
    defaultTopPadding = calculateTopPaddingDefault(mapView);
    waynameTopPadding = calculateTopPaddingWithWayname(mapView.getContext(), defaultTopPadding);
  }

  void updateTopPaddingWithWayname() {
    updateTopPadding(waynameTopPadding);
  }

  void updateTopPaddingWithDefault() {
    updateTopPadding(defaultTopPadding);
  }

  void removeAllPadding() {
    updatePadding(ZERO_MAP_PADDING);
  }

  private int calculateTopPaddingDefault(MapView mapView) {
    Context context = mapView.getContext();
    Resources resources = context.getResources();
    int mapViewHeight = mapView.getHeight();
    int bottomSheetHeight = (int) resources.getDimension(R.dimen.summary_bottomsheet_height);
    return mapViewHeight - (bottomSheetHeight * BOTTOMSHEET_PADDING_MULTIPLIER);
  }

  private int calculateTopPaddingWithWayname(Context context, int defaultTopPadding) {
    Resources resources = context.getResources();
    int waynameLayoutHeight = (int) resources.getDimension(R.dimen.wayname_view_height);
    return defaultTopPadding - (waynameLayoutHeight * WAYNAME_PADDING_MULTIPLIER);
  }

  private void updatePadding(int[] padding) {
    mapboxMap.setPadding(padding[0], padding[1], padding[2], padding[3]);
  }

  private void updateTopPadding(int topPadding) {
    mapboxMap.setPadding(0, topPadding, 0, 0);
  }
}
