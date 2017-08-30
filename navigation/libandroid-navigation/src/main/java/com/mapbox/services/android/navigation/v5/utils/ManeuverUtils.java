package com.mapbox.services.android.navigation.v5.utils;

import com.mapbox.services.android.navigation.R;
import com.mapbox.services.api.directions.v5.models.LegStep;
import com.mapbox.services.api.directions.v5.models.StepManeuver;
import com.mapbox.services.commons.utils.TextUtils;

public class ManeuverUtils {

  public static int getManeuverResource(LegStep step) {
    ManeuverMap maneuverMap = new ManeuverMap();
    if (step != null && step.getManeuver() != null) {
      StepManeuver maneuver = step.getManeuver();
      if (!TextUtils.isEmpty(maneuver.getModifier())) {
        return maneuverMap.getManeuverResource(maneuver.getType() + maneuver.getModifier());
      } else {
        return maneuverMap.getManeuverResource(maneuver.getType());
      }
    }
    return R.drawable.ic_maneuver_turn_0;
  }
}
