package com.mapbox.services.android.navigation.ui.v5.map;

import android.content.Context;

import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.services.android.navigation.ui.v5.camera.NavigationCamera;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteLegProgress;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.lang.ref.WeakReference;

class MapFpsDelegate {

  private static final int VALID_DURATION_IN_SECONDS_UNTIL_NEXT_MANEUVER = 7;
  private static final int VALID_DURATION_IN_SECONDS_SINCE_PREVIOUS_MANEUVER = 3;
  private static final int PLUGGED_IN_MAX_FPS = 120;
  static final int DEFAULT_MAX_FPS = 20;

  private final WeakReference<MapView> mapViewWeakReference;
  private final MapBatteryMonitor batteryMonitor;
  private final ProgressChangeListener fpsProgressListener = new FpsDelegateProgressChangeListener(this);
  private MapboxNavigation navigation;
  private int maxFps = DEFAULT_MAX_FPS;
  private boolean isTracking;
  private boolean isEnabled = true;

  MapFpsDelegate(MapView mapView, MapBatteryMonitor batteryMonitor) {
    this.mapViewWeakReference = new WeakReference<>(mapView);
    this.batteryMonitor = batteryMonitor;
  }

  void addProgressChangeListener(MapboxNavigation navigation) {
    this.navigation = navigation;
    navigation.addProgressChangeListener(fpsProgressListener);
  }

  void onStart() {
    if (navigation != null) {
      navigation.addProgressChangeListener(fpsProgressListener);
    }
  }

  void onStop() {
    if (navigation != null) {
      navigation.removeProgressChangeListener(fpsProgressListener);
    }
  }

  void updateEnabled(boolean isEnabled) {
    this.isEnabled = isEnabled;
    if (!isEnabled) {
      resetMaxFps();
    }
  }

  boolean isEnabled() {
    return isEnabled;
  }

  void updateMaxFps(int maxFps) {
    this.maxFps = maxFps;
  }

  int retrieveMaxFps() {
    return maxFps;
  }

  void updateCameraTracking(@NavigationCamera.TrackingMode int trackingMode) {
    isTracking = trackingMode != NavigationCamera.NAVIGATION_TRACKING_MODE_NONE;
    if (!isTracking) {
      resetMaxFps();
    }
  }

  void adjustFpsFor(RouteProgress routeProgress) {
    MapView mapView = mapViewWeakReference.get();
    if (mapView == null || !isEnabled || !isTracking) {
      return;
    }

    int maxFps = determineMaxFpsFrom(routeProgress, mapView.getContext());
    mapView.setMaximumFps(maxFps);
  }

  private void resetMaxFps() {
    MapView mapView = mapViewWeakReference.get();
    if (mapView != null) {
      mapView.setMaximumFps(PLUGGED_IN_MAX_FPS);
    }
  }

  private int determineMaxFpsFrom(RouteProgress routeProgress, Context context) {
    final boolean isPluggedIn = batteryMonitor.isPluggedIn(context);
    RouteLegProgress routeLegProgress = routeProgress.currentLegProgress();

    if (isPluggedIn) {
      return PLUGGED_IN_MAX_FPS;
    } else if (validLowFpsManeuver(routeLegProgress) || validLowFpsDuration(routeLegProgress)) {
      return maxFps;
    } else {
      return PLUGGED_IN_MAX_FPS;
    }
  }

  private boolean validLowFpsManeuver(RouteLegProgress routeLegProgress) {
    final String maneuverModifier = routeLegProgress.currentStep().maneuver().modifier();
    return maneuverModifier != null
      && (maneuverModifier.equals(NavigationConstants.STEP_MANEUVER_MODIFIER_STRAIGHT)
      || maneuverModifier.equals(NavigationConstants.STEP_MANEUVER_MODIFIER_SLIGHT_LEFT)
      || maneuverModifier.equals(NavigationConstants.STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT));
  }

  private boolean validLowFpsDuration(RouteLegProgress routeLegProgress) {
    final double expectedStepDuration = routeLegProgress.currentStep().duration();
    final double durationUntilNextManeuver = routeLegProgress.currentStepProgress().durationRemaining();
    final double durationSincePreviousManeuver = expectedStepDuration - durationUntilNextManeuver;
    return durationUntilNextManeuver > VALID_DURATION_IN_SECONDS_UNTIL_NEXT_MANEUVER
      && durationSincePreviousManeuver > VALID_DURATION_IN_SECONDS_SINCE_PREVIOUS_MANEUVER;
  }
}
