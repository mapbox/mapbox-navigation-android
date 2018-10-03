package com.mapbox.services.android.navigation.ui.v5.route;

import android.os.AsyncTask;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.core.constants.Constants;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.PRIMARY_ROUTE_PROPERTY_KEY;

class FeatureProcessingTask extends AsyncTask<Void, Void, Void> {

  private final List<DirectionsRoute> routes;
  private final OnRouteFeaturesProcessedCallback callback;
  private final List<FeatureCollection> routeFeatureCollections = new ArrayList<>();
  private final HashMap<LineString, DirectionsRoute> routeLineStrings = new HashMap<>();

  FeatureProcessingTask(List<DirectionsRoute> routes, OnRouteFeaturesProcessedCallback callback) {
    this.routes = routes;
    this.callback = callback;
  }

  @Override
  protected Void doInBackground(Void... voids) {
    for (int i = 0; i < routes.size(); i++) {
      DirectionsRoute route = routes.get(i);
      boolean isPrimary = i == 0;
      FeatureCollection routeFeatureCollection = createRouteFeatureCollection(route, isPrimary);
      routeFeatureCollections.add(routeFeatureCollection);
    }
    return null;
  }

  @Override
  protected void onPostExecute(Void result) {
    super.onPostExecute(result);
    callback.onRouteFeaturesProcessed(routeFeatureCollections, routeLineStrings);
  }

  private FeatureCollection createRouteFeatureCollection(DirectionsRoute route, boolean isPrimary) {
    final List<Feature> features = new ArrayList<>();

    LineString routeGeometry = LineString.fromPolyline(route.geometry(), Constants.PRECISION_6);
    Feature routeFeature = Feature.fromGeometry(routeGeometry);
    routeFeature.addBooleanProperty(PRIMARY_ROUTE_PROPERTY_KEY, isPrimary);
    features.add(routeFeature);
    routeLineStrings.put(routeGeometry, route);

    List<Feature> congestionFeatures = buildCongestionFeaturesFromRoute(route, routeGeometry, isPrimary);
    features.addAll(congestionFeatures);
    return FeatureCollection.fromFeatures(features);
  }

  private List<Feature> buildCongestionFeaturesFromRoute(DirectionsRoute route, LineString lineString,
                                                         boolean isPrimary) {
    final List<Feature> features = new ArrayList<>();
    for (RouteLeg leg : route.legs()) {
      if (leg.annotation() != null && leg.annotation().congestion() != null) {
        for (int i = 0; i < leg.annotation().congestion().size(); i++) {
          // See https://github.com/mapbox/mapbox-navigation-android/issues/353
          if (leg.annotation().congestion().size() + 1 <= lineString.coordinates().size()) {

            List<Point> points = new ArrayList<>();
            points.add(lineString.coordinates().get(i));
            points.add(lineString.coordinates().get(i + 1));

            LineString congestionLineString = LineString.fromLngLats(points);
            Feature feature = Feature.fromGeometry(congestionLineString);
            String congestionValue = leg.annotation().congestion().get(i);
            feature.addStringProperty(RouteConstants.CONGESTION_KEY, congestionValue);
            feature.addBooleanProperty(PRIMARY_ROUTE_PROPERTY_KEY, isPrimary);
            features.add(feature);
          }
        }
      } else {
        Feature feature = Feature.fromGeometry(lineString);
        features.add(feature);
      }
    }
    return features;
  }
}
