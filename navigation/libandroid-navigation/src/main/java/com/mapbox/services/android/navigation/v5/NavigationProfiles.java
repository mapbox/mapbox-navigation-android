package com.mapbox.services.android.navigation.v5;

import android.support.annotation.StringDef;

import com.mapbox.services.api.directions.v5.DirectionsCriteria;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class NavigationProfiles {

  /**
   * One of these constants should be used when the navigation profiles being changed.
   *
   * @since 0.3.0
   */
  @StringDef( {DirectionsCriteria.PROFILE_DRIVING, DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
    DirectionsCriteria.PROFILE_CYCLING, DirectionsCriteria.PROFILE_WALKING})
  @Retention(RetentionPolicy.SOURCE)
  public @interface Profile {
  }
}