package com.mapbox.services.android.navigation.ui.v5.instruction.maneuver;

import android.graphics.Canvas;
import android.graphics.PointF;

import androidx.core.util.Pair;

import java.util.HashMap;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_SHARP_LEFT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_SHARP_RIGHT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_SLIGHT_LEFT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_STRAIGHT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_UTURN;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_ARRIVE;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_EXIT_ROTARY;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_EXIT_ROUNDABOUT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_FORK;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_MERGE;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_OFF_RAMP;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_ROTARY;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT_TURN;

class ManeuverViewMap extends HashMap<Pair<String, String>, ManeuverViewUpdate> {

  ManeuverViewMap() {
    put(new Pair<String, String>(STEP_MANEUVER_TYPE_MERGE, null), new ManeuverViewUpdate() {
      @Override
      public void updateManeuverView(Canvas canvas, int primaryColor, int secondaryColor,
                                     PointF size, float roundaboutAngle) {
        ManeuversStyleKit.drawMerge(canvas, primaryColor, secondaryColor, size);
      }
    });
    put(new Pair<String, String>(STEP_MANEUVER_TYPE_OFF_RAMP, null), new ManeuverViewUpdate() {
      @Override
      public void updateManeuverView(Canvas canvas, int primaryColor, int secondaryColor,
                                     PointF size, float roundaboutAngle) {
        ManeuversStyleKit.drawOffRamp(canvas, primaryColor, secondaryColor, size);
      }
    });
    put(new Pair<String, String>(STEP_MANEUVER_TYPE_FORK, null), new ManeuverViewUpdate() {
      @Override
      public void updateManeuverView(Canvas canvas, int primaryColor, int secondaryColor,
                                     PointF size, float roundaboutAngle) {
        ManeuversStyleKit.drawFork(canvas, primaryColor, secondaryColor, size);
      }
    });
    put(new Pair<String, String>(STEP_MANEUVER_TYPE_ROUNDABOUT, null), new ManeuverViewUpdate() {
      @Override
      public void updateManeuverView(Canvas canvas, int primaryColor, int secondaryColor,
                                     PointF size, float roundaboutAngle) {
        ManeuversStyleKit.drawRoundabout(canvas, primaryColor, secondaryColor, size, roundaboutAngle);
      }
    });
    put(new Pair<String, String>(STEP_MANEUVER_TYPE_ROUNDABOUT_TURN, null), new ManeuverViewUpdate() {
      @Override
      public void updateManeuverView(Canvas canvas, int primaryColor, int secondaryColor,
                                     PointF size, float roundaboutAngle) {
        ManeuversStyleKit.drawRoundabout(canvas, primaryColor, secondaryColor, size, roundaboutAngle);
      }
    });
    put(new Pair<String, String>(STEP_MANEUVER_TYPE_EXIT_ROUNDABOUT, null), new ManeuverViewUpdate() {
      @Override
      public void updateManeuverView(Canvas canvas, int primaryColor, int secondaryColor,
                                     PointF size, float roundaboutAngle) {
        ManeuversStyleKit.drawRoundabout(canvas, primaryColor, secondaryColor, size, roundaboutAngle);
      }
    });
    put(new Pair<String, String>(STEP_MANEUVER_TYPE_ROTARY, null), new ManeuverViewUpdate() {
      @Override
      public void updateManeuverView(Canvas canvas, int primaryColor, int secondaryColor,
                                     PointF size, float roundaboutAngle) {
        ManeuversStyleKit.drawRoundabout(canvas, primaryColor, secondaryColor, size, roundaboutAngle);
      }
    });
    put(new Pair<String, String>(STEP_MANEUVER_TYPE_EXIT_ROTARY, null), new ManeuverViewUpdate() {
      @Override
      public void updateManeuverView(Canvas canvas, int primaryColor, int secondaryColor,
                                     PointF size, float roundaboutAngle) {
        ManeuversStyleKit.drawRoundabout(canvas, primaryColor, secondaryColor, size, roundaboutAngle);
      }
    });
    put(new Pair<String, String>(STEP_MANEUVER_TYPE_ARRIVE, null), new ManeuverViewUpdate() {
      @Override
      public void updateManeuverView(Canvas canvas, int primaryColor, int secondaryColor,
                                     PointF size, float roundaboutAngle) {
        ManeuversStyleKit.drawArrive(canvas, primaryColor, size);
      }
    });
    put(new Pair<>(STEP_MANEUVER_TYPE_ARRIVE, STEP_MANEUVER_MODIFIER_STRAIGHT), new ManeuverViewUpdate() {
      @Override
      public void updateManeuverView(Canvas canvas, int primaryColor, int secondaryColor,
                                     PointF size, float roundaboutAngle) {
        ManeuversStyleKit.drawArrive(canvas, primaryColor, size);
      }
    });
    put(new Pair<>(STEP_MANEUVER_TYPE_ARRIVE, STEP_MANEUVER_MODIFIER_RIGHT), new ManeuverViewUpdate() {
      @Override
      public void updateManeuverView(Canvas canvas, int primaryColor, int secondaryColor,
                                     PointF size, float roundaboutAngle) {
        ManeuversStyleKit.drawArriveRight(canvas, primaryColor, size);
      }
    });
    put(new Pair<>(STEP_MANEUVER_TYPE_ARRIVE, STEP_MANEUVER_MODIFIER_LEFT), new ManeuverViewUpdate() {
      @Override
      public void updateManeuverView(Canvas canvas, int primaryColor, int secondaryColor,
                                     PointF size, float roundaboutAngle) {
        ManeuversStyleKit.drawArriveRight(canvas, primaryColor, size);
      }
    });
    put(new Pair<String, String>(null, STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT), new ManeuverViewUpdate() {
      @Override
      public void updateManeuverView(Canvas canvas, int primaryColor, int secondaryColor,
                                     PointF size, float roundaboutAngle) {
        ManeuversStyleKit.drawArrowSlightRight(canvas, primaryColor, size);
      }
    });
    put(new Pair<String, String>(null, STEP_MANEUVER_MODIFIER_RIGHT), new ManeuverViewUpdate() {
      @Override
      public void updateManeuverView(Canvas canvas, int primaryColor, int secondaryColor,
                                     PointF size, float roundaboutAngle) {
        ManeuversStyleKit.drawArrowRight(canvas, primaryColor, size);
      }
    });
    put(new Pair<String, String>(null, STEP_MANEUVER_MODIFIER_SHARP_RIGHT), new ManeuverViewUpdate() {
      @Override
      public void updateManeuverView(Canvas canvas, int primaryColor, int secondaryColor,
                                     PointF size, float roundaboutAngle) {
        ManeuversStyleKit.drawArrowSharpRight(canvas, primaryColor, size);
      }
    });
    put(new Pair<String, String>(null, STEP_MANEUVER_MODIFIER_SLIGHT_LEFT), new ManeuverViewUpdate() {
      @Override
      public void updateManeuverView(Canvas canvas, int primaryColor, int secondaryColor,
                                     PointF size, float roundaboutAngle) {
        ManeuversStyleKit.drawArrowSlightRight(canvas, primaryColor, size);
      }
    });
    put(new Pair<String, String>(null, STEP_MANEUVER_MODIFIER_LEFT), new ManeuverViewUpdate() {
      @Override
      public void updateManeuverView(Canvas canvas, int primaryColor, int secondaryColor,
                                     PointF size, float roundaboutAngle) {
        ManeuversStyleKit.drawArrowRight(canvas, primaryColor, size);
      }
    });
    put(new Pair<String, String>(null, STEP_MANEUVER_MODIFIER_SHARP_LEFT), new ManeuverViewUpdate() {
      @Override
      public void updateManeuverView(Canvas canvas, int primaryColor, int secondaryColor,
                                     PointF size, float roundaboutAngle) {
        ManeuversStyleKit.drawArrowSharpRight(canvas, primaryColor, size);
      }
    });
    put(new Pair<String, String>(null, STEP_MANEUVER_MODIFIER_UTURN), new ManeuverViewUpdate() {
      @Override
      public void updateManeuverView(Canvas canvas, int primaryColor, int secondaryColor,
                                     PointF size, float roundaboutAngle) {
        ManeuversStyleKit.drawArrow180Right(canvas, primaryColor, size);
      }
    });
    put(new Pair<String, String>(null, STEP_MANEUVER_MODIFIER_STRAIGHT), new ManeuverViewUpdate() {
      @Override
      public void updateManeuverView(Canvas canvas, int primaryColor, int secondaryColor,
                                     PointF size, float roundaboutAngle) {
        ManeuversStyleKit.drawArrowStraight(canvas, primaryColor, size);
      }
    });
    put(new Pair<String, String>(null, null), new ManeuverViewUpdate() {
      @Override
      public void updateManeuverView(Canvas canvas, int primaryColor, int secondaryColor,
                                     PointF size, float roundaboutAngle) {
        ManeuversStyleKit.drawArrowStraight(canvas, primaryColor, size);
      }
    });
  }
}
