package com.mapbox.services.android.navigation.ui.v5.route;

import android.support.annotation.NonNull;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.turf.TurfConstants;
import com.mapbox.turf.TurfMeasurement;
import com.mapbox.turf.TurfMisc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

class MapRouteClickListener implements MapboxMap.OnMapClickListener {

  private final MapRouteLine routeLine;

  private OnRouteSelectionChangeListener onRouteSelectionChangeListener;
  private boolean alternativesVisible = true;

  MapRouteClickListener(MapRouteLine routeLine) {
    this.routeLine = routeLine;
  }

  @Override
  public void onMapClick(@NonNull LatLng point) {
    HashMap<LineString, DirectionsRoute> routeLineStrings = routeLine.retrieveRouteLineStrings();
    if (invalidMapClick(routeLineStrings)) {
      return;
    }
    List<DirectionsRoute> directionsRoutes = routeLine.retrieveDirectionsRoutes();
    findClickedRoute(point, routeLineStrings, directionsRoutes);
  }

  void setOnRouteSelectionChangeListener(OnRouteSelectionChangeListener listener) {
    onRouteSelectionChangeListener = listener;
  }

  void updateAlternativesVisible(boolean alternativesVisible) {
    this.alternativesVisible = alternativesVisible;
  }

  private boolean invalidMapClick(HashMap<LineString, DirectionsRoute> routeLineStrings) {
    return routeLineStrings == null || routeLineStrings.isEmpty() || !alternativesVisible;
  }

  private void findClickedRoute(@NonNull LatLng point, HashMap<LineString, DirectionsRoute> routeLineStrings,
                                List<DirectionsRoute> directionsRoutes) {

    HashMap<Double, DirectionsRoute> routeDistancesAwayFromClick = new HashMap<>();
    Point clickPoint = Point.fromLngLat(point.getLongitude(), point.getLatitude());

    calculateClickDistances(routeDistancesAwayFromClick, clickPoint, routeLineStrings);
    List<Double> distancesAwayFromClick = new ArrayList<>(routeDistancesAwayFromClick.keySet());
    Collections.sort(distancesAwayFromClick);

    DirectionsRoute clickedRoute = routeDistancesAwayFromClick.get(distancesAwayFromClick.get(0));
    int newPrimaryRouteIndex = directionsRoutes.indexOf(clickedRoute);
    if (routeLine.updatePrimaryRouteIndex(newPrimaryRouteIndex) && onRouteSelectionChangeListener != null) {
      DirectionsRoute selectedRoute = directionsRoutes.get(newPrimaryRouteIndex);
      onRouteSelectionChangeListener.onNewPrimaryRouteSelected(selectedRoute);
    }
  }

  private void calculateClickDistances(HashMap<Double, DirectionsRoute> routeDistancesAwayFromClick,
                                          Point clickPoint, HashMap<LineString, DirectionsRoute> routeLineStrings) {
    for (LineString lineString : routeLineStrings.keySet()) {
      Point pointOnLine = findPointOnLine(clickPoint, lineString);
      if (pointOnLine == null) {
        return;
      }
      double distance = TurfMeasurement.distance(clickPoint, pointOnLine, TurfConstants.UNIT_METERS);
      routeDistancesAwayFromClick.put(distance, routeLineStrings.get(lineString));
    }
  }

  private Point findPointOnLine(Point clickPoint, LineString lineString) {
    List<Point> linePoints = lineString.coordinates();
    Feature feature = TurfMisc.nearestPointOnLine(clickPoint, linePoints);
    return (Point) feature.geometry();
  }
}
