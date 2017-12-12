package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.SparseArray;

class BannerShieldInfo {
  private String url;
  private String text;
  private int startIndex;

  BannerShieldInfo(Context context, String url, int startIndex, String text) {
    int densityDpi = context.getResources().getDisplayMetrics().densityDpi;
    String densityFormat = new BannerDensityMap().get(densityDpi);
    this.url = url + densityFormat;
    this.startIndex = startIndex;
    this.text = text;
  }

  String getUrl() {
    return url;
  }

  public String getText() {
    return text;
  }

  int getStartIndex() {
    return startIndex;
  }

  int getEndIndex() {
    return startIndex + 1;
  }

  private static class BannerDensityMap extends SparseArray<String> {

    private static final String ONE_X = "@1x";
    private static final String TWO_X = "@2x";
    private static final String THREE_X = "@3x";
    private static final String FOUR_X = "@4x";
    private static final String DOT_PNG = ".png";

    BannerDensityMap() {
      super(4);
      put(DisplayMetrics.DENSITY_MEDIUM, ONE_X + DOT_PNG);
      put(DisplayMetrics.DENSITY_HIGH, TWO_X + DOT_PNG);
      put(DisplayMetrics.DENSITY_XHIGH, THREE_X + DOT_PNG);
      put(DisplayMetrics.DENSITY_XXHIGH, THREE_X + DOT_PNG);
      put(DisplayMetrics.DENSITY_XXXHIGH, FOUR_X + DOT_PNG);
    }
  }
}
