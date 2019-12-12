package com.mapbox.navigation.ui.instruction.maneuver;

import android.graphics.Canvas;
import android.graphics.PointF;

interface ManeuverViewUpdate {
  void updateManeuverView(Canvas canvas, int primaryColor, int secondaryColor, PointF size, float roundaboutAngle);
}
