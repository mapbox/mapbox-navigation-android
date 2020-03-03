package com.mapbox.services.android.navigation.v5.internal.navigation.maneuver;

import android.graphics.Canvas;
import android.graphics.PointF;

public interface ManeuverIconDrawer {
  void drawManeuverIcon(Canvas canvas, int primaryColor, int secondaryColor, PointF size, float roundaboutAngle);
}
