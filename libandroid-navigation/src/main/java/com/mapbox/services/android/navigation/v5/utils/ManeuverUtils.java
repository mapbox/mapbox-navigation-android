package com.mapbox.services.android.navigation.v5.utils;

import android.text.TextUtils;

import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.StepManeuver;
import com.mapbox.services.android.navigation.R;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT;

public class ManeuverUtils {
  private static final String EMPTY = "";

  public static int getManeuverResource(LegStep step) {
    ManeuverMap maneuverMap = new ManeuverMap();
    if (step != null && step.maneuver() != null) {
      StepManeuver maneuver = step.maneuver();
      if (!TextUtils.isEmpty(maneuver.modifier())) {
        String drivingSide = step.drivingSide();
        if (!STEP_MANEUVER_MODIFIER_LEFT.equals(drivingSide)) {
          drivingSide = EMPTY;
        }
        return maneuverMap.getManeuverResource(maneuver.type() + maneuver.modifier() + drivingSide);
      } else {
        return maneuverMap.getManeuverResource(maneuver.type());
      }
    }
    return R.drawable.ic_maneuver_turn_0;
  }
}
