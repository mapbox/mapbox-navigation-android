package com.mapbox.services.android.navigation.v5.route;

import android.location.Location;

import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.v5.navigation.NavigationUnitType;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.util.Locale;

public interface RouteEngine {

  void updateAccessToken(String accessToken);

  String obtainAccessToken();

  void updateLocale(Locale locale);

  Locale obtainLocale();

  void updateUnitType(@NavigationUnitType.UnitType int unitType);

  int obtainUnitType();

  void updateRouteProfile(String routeProfile);

  String obtainRouteProfile();

  void findRouteFromRouteProgress(Location location, RouteProgress routeProgress);

  void findRouteFromOriginToDestination(Point origin, Point destination);

  void addRouteEngineListener(RouteEngineListener listener);

  void removeRouteEngineListener(RouteEngineListener listener);
}
