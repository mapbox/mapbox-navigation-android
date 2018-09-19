package com.mapbox.services.android.navigation.ui.v5;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class CameraState {
  @Retention(RetentionPolicy.SOURCE)
  @IntDef( {TRACKING, NOT_TRACKING, OVERVIEW})
  public @interface Type {
  }

  public static final int TRACKING = 0;
  public static final int NOT_TRACKING = 1;
  public static final int OVERVIEW = 2;
}
