package com.mapbox.services.android.navigation.ui.v5;

import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.view.View;

import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;

import org.hamcrest.Matcher;

import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;

public class NavigationMapRouteAction implements ViewAction {

  private MapboxMap mapboxMap;
  private NavigationMapRoute navigationMapRoute;
  private OnPerformNavigationMapRouteAction onPerformNavigationMapRouteAction;

  NavigationMapRouteAction(MapboxMap mapboxMap, NavigationMapRoute navigationMapRoute,
                           OnPerformNavigationMapRouteAction onPerformNavigationMapRouteAction) {
    this.navigationMapRoute = navigationMapRoute;
    this.mapboxMap = mapboxMap;
    this.onPerformNavigationMapRouteAction = onPerformNavigationMapRouteAction;
  }

  @Override
  public Matcher<View> getConstraints() {
    return isDisplayed();
  }

  @Override
  public String getDescription() {
    return getClass().getSimpleName();
  }

  @Override
  public void perform(UiController uiController, View view) {
    if (onPerformNavigationMapRouteAction != null) {
      onPerformNavigationMapRouteAction.onNavigationMapRouteAction(navigationMapRoute, mapboxMap, uiController);
    }
  }

  interface OnPerformNavigationMapRouteAction {
    void onNavigationMapRouteAction(NavigationMapRoute navigationMapRoute, MapboxMap mapboxMap,
                                    UiController uiController);
  }
}
