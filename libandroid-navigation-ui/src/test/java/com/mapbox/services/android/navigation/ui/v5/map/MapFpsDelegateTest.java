package com.mapbox.services.android.navigation.ui.v5.map;

import android.content.Context;

import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.StepManeuver;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.services.android.navigation.ui.v5.camera.NavigationCamera;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteLegProgress;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteStepProgress;

import org.junit.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MapFpsDelegateTest {

  @Test
  public void addProgressChangeListener_navigationReceivesListener() {
    MapboxNavigation navigation = mock(MapboxNavigation.class);
    MapFpsDelegate delegate = new MapFpsDelegate(mock(MapView.class), mock(MapBatteryMonitor.class));

    delegate.addProgressChangeListener(navigation);

    verify(navigation).addProgressChangeListener(any(FpsDelegateProgressChangeListener.class));
  }

  @Test
  public void onTransitionFinished_resetFpsWhenNotTracking() {
    MapView mapView = mock(MapView.class);
    MapFpsDelegate delegate = new MapFpsDelegate(mapView, mock(MapBatteryMonitor.class));

    delegate.onTransitionFinished(NavigationCamera.NAVIGATION_TRACKING_MODE_NONE);

    verify(mapView).setMaximumFps(eq(Integer.MAX_VALUE));
  }

  @Test
  public void onTransitionCancelled_resetFpsWhenNotTracking() {
    MapView mapView = mock(MapView.class);
    MapFpsDelegate delegate = new MapFpsDelegate(mapView, mock(MapBatteryMonitor.class));

    delegate.onTransitionCancelled(NavigationCamera.NAVIGATION_TRACKING_MODE_NONE);

    verify(mapView).setMaximumFps(eq(Integer.MAX_VALUE));
  }

  @Test
  public void onStop_navigationListenerRemoved() {
    MapboxNavigation navigation = mock(MapboxNavigation.class);
    MapFpsDelegate delegate = new MapFpsDelegate(mock(MapView.class), mock(MapBatteryMonitor.class));
    delegate.addProgressChangeListener(navigation);

    delegate.onStop();

    verify(navigation).removeProgressChangeListener(any(FpsDelegateProgressChangeListener.class));
  }

  @Test
  public void updateEnabledFalse_maxFpsReset() {
    MapView mapView = mock(MapView.class);
    MapFpsDelegate delegate = new MapFpsDelegate(mapView, mock(MapBatteryMonitor.class));

    delegate.updateEnabled(false);

    mapView.setMaximumFps(eq(Integer.MAX_VALUE));
  }

  @Test
  public void adjustFpsFor_thresholdSetWithCorrectManeuver() {
    MapView mapView = mock(MapView.class);
    MapBatteryMonitor batteryMonitor = mock(MapBatteryMonitor.class);
    when(batteryMonitor.isPluggedIn(any(Context.class))).thenReturn(false);
    MapFpsDelegate delegate = new MapFpsDelegate(mapView, batteryMonitor);
    RouteProgress routeProgress = buildRouteProgressWith("straight");
    int maxFps = 5;
    delegate.updateMaxFpsThreshold(maxFps);

    delegate.adjustFpsFor(routeProgress);

    verify(mapView).setMaximumFps(eq(maxFps));
  }

  @Test
  public void adjustFpsFor_thresholdSetWithCorrectDuration() {
    MapView mapView = mock(MapView.class);
    MapBatteryMonitor batteryMonitor = mock(MapBatteryMonitor.class);
    when(batteryMonitor.isPluggedIn(any(Context.class))).thenReturn(false);
    MapFpsDelegate delegate = new MapFpsDelegate(mapView, batteryMonitor);
    RouteProgress routeProgress = buildRouteProgressWith(100d, 20d);
    int maxFps = 5;
    delegate.updateMaxFpsThreshold(maxFps);

    delegate.adjustFpsFor(routeProgress);

    verify(mapView).setMaximumFps(eq(maxFps));
  }

  private RouteProgress buildRouteProgressWith(String maneuverModifier) {
    RouteProgress routeProgress = mock(RouteProgress.class);
    RouteLegProgress routeLegProgress = mock(RouteLegProgress.class);
    LegStep currentStep = mock(LegStep.class);
    StepManeuver currentManeuver = mock(StepManeuver.class);
    when(currentManeuver.modifier()).thenReturn(maneuverModifier);
    when(currentStep.maneuver()).thenReturn(currentManeuver);
    when(routeLegProgress.currentStep()).thenReturn(currentStep);
    when(routeProgress.currentLegProgress()).thenReturn(routeLegProgress);
    return routeProgress;
  }

  private RouteProgress buildRouteProgressWith(double totalDuration, double durationRemaining) {
    RouteProgress routeProgress = mock(RouteProgress.class);
    RouteLegProgress routeLegProgress = mock(RouteLegProgress.class);
    RouteStepProgress routeStepProgress = mock(RouteStepProgress.class);
    StepManeuver currentManeuver = mock(StepManeuver.class);
    when(currentManeuver.modifier()).thenReturn("left");
    LegStep currentStep = mock(LegStep.class);
    when(currentStep.duration()).thenReturn(totalDuration);
    when(routeStepProgress.durationRemaining()).thenReturn(durationRemaining);
    when(routeLegProgress.currentStepProgress()).thenReturn(routeStepProgress);
    when(routeProgress.currentLegProgress()).thenReturn(routeLegProgress);
    when(currentStep.maneuver()).thenReturn(currentManeuver);
    when(routeLegProgress.currentStep()).thenReturn(currentStep);
    return routeProgress;
  }
}