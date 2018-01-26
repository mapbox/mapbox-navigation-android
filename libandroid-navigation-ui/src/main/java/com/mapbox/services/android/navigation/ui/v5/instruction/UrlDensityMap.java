package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.SparseArray;

class UrlDensityMap extends SparseArray<String> {

  private static final String ONE_X = "@1x";
  private static final String TWO_X = "@2x";
  private static final String THREE_X = "@3x";
  private static final String FOUR_X = "@4x";
  private static final String DOT_PNG = ".png";

  private int displayDensity;

  UrlDensityMap(Context context) {
    super(4);
    displayDensity = context.getResources().getDisplayMetrics().densityDpi;
    put(DisplayMetrics.DENSITY_MEDIUM, ONE_X + DOT_PNG);
    put(DisplayMetrics.DENSITY_HIGH, TWO_X + DOT_PNG);
    put(DisplayMetrics.DENSITY_XHIGH, THREE_X + DOT_PNG);
    put(DisplayMetrics.DENSITY_XXHIGH, THREE_X + DOT_PNG);
    put(DisplayMetrics.DENSITY_XXXHIGH, FOUR_X + DOT_PNG);
  }

  public String get(String url) {
    return url + super.get(displayDensity);
  }
}