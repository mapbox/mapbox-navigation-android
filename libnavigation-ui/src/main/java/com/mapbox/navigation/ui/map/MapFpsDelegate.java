package com.mapbox.navigation.ui.map;

import android.content.Context;

import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.navigation.base.trip.model.RouteLegProgress;
import com.mapbox.navigation.base.trip.model.RouteProgress;
import com.mapbox.navigation.core.MapboxNavigation;
import com.mapbox.navigation.core.trip.session.RouteProgressObserver;
import com.mapbox.navigation.ui.camera.NavigationCamera;
import com.mapbox.navigation.ui.camera.OnTrackingModeChangedListener;
import com.mapbox.navigation.ui.camera.OnTrackingModeTransitionListener;
import com.mapbox.navigation.utils.ConstantsEx;

class MapFpsDelegate implements OnTrackingModeChangedListener, OnTrackingModeTransitionListener {

  static final int DEFAULT_MAX_FPS_THRESHOLD = 20;
  private static final double VALID_DURATION_IN_SECONDS_UNTIL_NEXT_MANEUVER = 7d;
  private static final double VALID_DURATION_IN_SECONDS_SINCE_PREVIOUS_MANEUVER = 5d;
  private static final int DEVICE_MAX_FPS = Integer.MAX_VALUE;
  private static final int LOW_POWER_MAX_FPS = 30;

  private final MapView mapView;
  private final MapBatteryMonitor batteryMonitor;
  private final RouteProgressObserver fpsProgressListener = new FpsDelegateProgressChangeListener(this);
  private MapboxNavigation navigation;
  private int maxFpsThreshold = DEFAULT_MAX_FPS_THRESHOLD;
  private boolean isTracking = true;
  private boolean isEnabled = true;

  MapFpsDelegate(MapView mapView, MapBatteryMonitor batteryMonitor) {
    this.mapView = mapView;
    this.batteryMonitor = batteryMonitor;
  }

  @Override
  public void onTrackingModeChanged(int trackingMode) {
    int trackingModeNone = NavigationCamera.NAVIGATION_TRACKING_MODE_NONE;
    if (trackingMode == trackingModeNone) {
      updateCameraTracking(trackingModeNone);
    }
  }

  @Override
  public void onTransitionFinished(int trackingMode) {
    updateCameraTracking(trackingMode);
  }

  @Override
  public void onTransitionCancelled(int trackingMode) {
    updateCameraTracking(trackingMode);
  }

  void addProgressChangeListener(MapboxNavigation navigation) {
    this.navigation = navigation;
    navigation.registerRouteProgressObserver(fpsProgressListener);
  }

  void onStart() {
    if (navigation != null) {
      navigation.registerRouteProgressObserver(fpsProgressListener);
    }
  }

  void onStop() {
    if (navigation != null) {
      navigation.unregisterRouteProgressObserver(fpsProgressListener);
    }
  }

  void updateEnabled(boolean isEnabled) {
    this.isEnabled = isEnabled;
    resetMaxFps(!isEnabled);
  }

  void updateMaxFpsThreshold(int maxFps) {
    this.maxFpsThreshold = maxFps;
  }

  void adjustFpsFor(RouteProgress routeProgress) {
    if (!isEnabled || !isTracking) {
      return;
    }

    int maxFps = determineMaxFpsFrom(routeProgress, mapView.getContext());
    mapView.setMaximumFps(maxFps);
  }

  private void updateCameraTracking(@NavigationCamera.TrackingMode int trackingMode) {
    isTracking = trackingMode != NavigationCamera.NAVIGATION_TRACKING_MODE_NONE;
    resetMaxFps(!isTracking);
  }

  private void resetMaxFps(boolean shouldReset) {
    if (shouldReset) {
      mapView.setMaximumFps(DEVICE_MAX_FPS);
    }
  }

  private int determineMaxFpsFrom(RouteProgress routeProgress, Context context) {
    final boolean isPluggedIn = batteryMonitor.isPluggedIn(context);
    RouteLegProgress routeLegProgress = routeProgress.currentLegProgress();

    if (isPluggedIn) {
      return LOW_POWER_MAX_FPS;
    } else if (validLowFpsManeuver(routeLegProgress) || validLowFpsDuration(routeLegProgress)) {
      return maxFpsThreshold;
    } else {
      return LOW_POWER_MAX_FPS;
    }
  }

  private boolean validLowFpsManeuver(RouteLegProgress routeLegProgress) {
    final String maneuverModifier = routeLegProgress.currentStepProgress().step().maneuver().modifier();
    return maneuverModifier != null
            && (maneuverModifier.equals(ConstantsEx.STEP_MANEUVER_MODIFIER_STRAIGHT)
            || maneuverModifier.equals(NavigationConstants.STEP_MANEUVER_MODIFIER_SLIGHT_LEFT)
            || maneuverModifier.equals(NavigationConstants.STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT));
  }

  private boolean validLowFpsDuration(RouteLegProgress routeLegProgress) {
    final double expectedStepDuration = routeLegProgress.currentStepProgress().step().duration();
    final double durationUntilNextManeuver = routeLegProgress.currentStepProgress().durationRemaining();
    final double durationSincePreviousManeuver = expectedStepDuration - durationUntilNextManeuver;
    return durationUntilNextManeuver > VALID_DURATION_IN_SECONDS_UNTIL_NEXT_MANEUVER
            && durationSincePreviousManeuver > VALID_DURATION_IN_SECONDS_SINCE_PREVIOUS_MANEUVER;
  }
}
