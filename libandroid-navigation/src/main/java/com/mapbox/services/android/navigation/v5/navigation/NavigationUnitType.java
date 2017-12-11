package com.mapbox.services.android.navigation.v5.navigation;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class NavigationUnitType {

  @Retention(RetentionPolicy.SOURCE)

  @IntDef( {TYPE_IMPERIAL, TYPE_METRIC})

  public @interface UnitType {
  }

  public static final int TYPE_IMPERIAL = 0;
  public static final int TYPE_METRIC = 1;
}
