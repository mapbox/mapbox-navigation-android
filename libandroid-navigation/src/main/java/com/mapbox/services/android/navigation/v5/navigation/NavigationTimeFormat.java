package com.mapbox.services.android.navigation.v5.navigation;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class NavigationTimeFormat {

  @Retention(RetentionPolicy.SOURCE)
  @IntDef( {NONE_SPECIFIED, TWELVE_HOURS, TWENTY_FOUR_HOURS})
  public @interface Type {
  }

  public static final int NONE_SPECIFIED = -1;
  public static final int TWELVE_HOURS = 0;
  public static final int TWENTY_FOUR_HOURS = 1;
}
