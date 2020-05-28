package com.mapbox.navigation.ui.internal.summary;

import android.text.SpannableString;

interface InstructionListView {

  void updateManeuverViewTypeAndModifier(String maneuverType, String maneuverModifier);

  void updateManeuverViewRoundaboutDegrees(float roundaboutAngle);

  void updateManeuverViewDrivingSide(String drivingSide);

  void updateDistanceText(SpannableString distanceText);

  void updatePrimaryText(String primaryText);

  void updatePrimaryMaxLines(int maxLines);

  void updateSecondaryText(String secondaryText);

  void updateSecondaryVisibility(int visibility);

  void updateBannerVerticalBias(float bias);

  void updateViewColors(int primaryTextColor, int secondaryTextColor,
                        int maneuverPrimaryColor, int maneuverSecondaryColor);
}
