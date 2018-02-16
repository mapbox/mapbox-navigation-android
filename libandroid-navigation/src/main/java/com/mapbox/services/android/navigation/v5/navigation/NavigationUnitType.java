package com.mapbox.services.android.navigation.v5.navigation;

import android.support.annotation.IntDef;

import com.mapbox.api.directions.v5.DirectionsCriteria;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class NavigationUnitType {

  @Retention(RetentionPolicy.SOURCE)

  @IntDef( {NONE_SPECIFIED, TYPE_IMPERIAL, TYPE_METRIC})

  public @interface UnitType {
  }

  public static final int NONE_SPECIFIED = -1;
  public static final int TYPE_IMPERIAL = 0;
  public static final int TYPE_METRIC = 1;

  public static String getDirectionsCriteriaUnitType(int unitType) {
    return unitType == NavigationUnitType.TYPE_IMPERIAL
      ? DirectionsCriteria.IMPERIAL : DirectionsCriteria.METRIC;
  }
}
