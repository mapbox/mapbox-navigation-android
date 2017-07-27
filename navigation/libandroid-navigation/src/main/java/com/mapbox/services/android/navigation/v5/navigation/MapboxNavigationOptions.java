package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.services.android.navigation.v5.NavigationProfiles;



public class MapboxNavigationOptions {

  private double maxTurnCompletionOffset;
  private double maneuverZoneRadius;

  private double maximumDistanceOffRoute;
  private double deadReckoningTimeInterval;
  private double maxManipulatedCourseAngle;

  private double userLocationSnapDistance;
  private int secondsBeforeReroute;

  private boolean defaultMilestonesEnabled = true;

  @NavigationProfiles.Profile
  private String profile;

}
