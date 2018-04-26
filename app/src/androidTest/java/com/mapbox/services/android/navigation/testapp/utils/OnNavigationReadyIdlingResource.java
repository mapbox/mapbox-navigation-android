package com.mapbox.services.android.navigation.testapp.utils;

import android.app.Activity;
import android.support.test.espresso.IdlingResource;

import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.ui.v5.NavigationView;
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions;
import com.mapbox.services.android.navigation.ui.v5.OnNavigationReadyCallback;

import java.lang.reflect.Field;

public class OnNavigationReadyIdlingResource implements IdlingResource, OnNavigationReadyCallback {

  private boolean isNavigationReady;
  private NavigationView navigationView;
  private ResourceCallback resourceCallback;

  public OnNavigationReadyIdlingResource(Activity activity) {
    try {
      Field field = activity.getClass().getDeclaredField("navigationView");
      field.setAccessible(true);
      navigationView = (NavigationView) field.get(activity);
      navigationView.initialize(this);
    } catch (Exception err) {
      throw new RuntimeException(err);
    }
  }

  @Override
  public String getName() {
    return getClass().getSimpleName();
  }

  @Override
  public boolean isIdleNow() {
    return isNavigationReady;
  }

  @Override
  public void registerIdleTransitionCallback(ResourceCallback resourceCallback) {
    this.resourceCallback = resourceCallback;
  }

  public NavigationView getNavigationView() {
    return navigationView;
  }

  @Override
  public void onNavigationReady() {
    navigationView.startNavigation(buildTestNavigationViewOptions());
    isNavigationReady = true;
    if (resourceCallback != null) {
      resourceCallback.onTransitionToIdle();
    }
  }

  private NavigationViewOptions buildTestNavigationViewOptions() {
    Point origin = Point.fromLngLat(-77.033987,38.900123);
    Point destination = Point.fromLngLat(-77.044818,38.848942);
    return NavigationViewOptions.builder()
      .origin(origin)
      .destination(destination)
      .shouldSimulateRoute(true)
      .build();
  }
}
