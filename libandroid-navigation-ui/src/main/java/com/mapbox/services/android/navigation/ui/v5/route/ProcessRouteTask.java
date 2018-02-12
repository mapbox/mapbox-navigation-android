package com.mapbox.services.android.navigation.ui.v5.route;

import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.core.constants.Constants;
import com.mapbox.services.android.navigation.ui.v5.utils.MapUtils;
import com.mapbox.services.commons.geojson.Feature;
import com.mapbox.services.commons.geojson.FeatureCollection;
import com.mapbox.services.commons.geojson.LineString;
import com.mapbox.services.commons.geojson.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


class ProcessRouteTask extends AsyncTask<List<DirectionsRoute>, Void, List<FeatureCollection>> {

  static final String SOURCE_KEY = "source";
  static final String CONGESTION_KEY = "congestion";
  static final String INDEX_KEY = "index";

  static final String WAYPOINT_SOURCE_ID = "mapbox-navigation-waypoint-source";
  static final String GENERIC_ROUTE_SOURCE_ID = "mapbox-navigation-route-source";
  static final String ID_FORMAT = "%s-%d";
  private static final String GENERIC_ROUTE_LAYER_ID = "mapbox-navigation-route-layer";
  private static final String GENERIC_ROUTE_SHIELD_LAYER_ID
    = "mapbox-navigation-route-shield-layer";

  private int primaryRouteIndex;
  private ProcessRouteListener listener;
  private

  public ProcessRouteTask(ProcessRouteListener listener, int primaryRouteIndex) {
    this.listener = listener;
    this.primaryRouteIndex = primaryRouteIndex;
  }

  @SafeVarargs
  @Override
  protected final List<FeatureCollection> doInBackground(List<DirectionsRoute>... lists) {
    return generateFeatureCollectionList(lists[0]);
  }

  @Override
  protected void onPostExecute(List<FeatureCollection> featureCollections) {
    super.onPostExecute(featureCollections);
    listener.onRouteProcessed(featureCollections);
  }

  private List<FeatureCollection> generateFeatureCollectionList(List<DirectionsRoute> directionsRoutes) {
    List<FeatureCollection> featureCollections = new ArrayList<>();
    // Each route contains traffic information and should be recreated considering this traffic
    // information.
    for (int i = 0; i < directionsRoutes.size(); i++) {
      featureCollections.add(addTrafficToSource(directionsRoutes.get(i), i));
    }

    // Add the waypoint geometries to represent them as an icon
    featureCollections.add(
      waypointFeatureCollection(directionsRoutes.get(primaryRouteIndex))
    );

    return featureCollections;
  }

  /**
   * The routes also display an icon for each waypoint in the route, we use symbol layers for this.
   */
  private static FeatureCollection waypointFeatureCollection(DirectionsRoute route) {
    final List<Feature> waypointFeatures = new ArrayList<>();
    for (RouteLeg leg : route.legs()) {
      waypointFeatures.add(getPointFromLineString(leg, 0));
      waypointFeatures.add(getPointFromLineString(leg, leg.steps().size() - 1));
    }
    return FeatureCollection.fromFeatures(waypointFeatures);
  }

  /**
   * If the {@link DirectionsRoute} request contains congestion information via annotations, breakup
   * the source into pieces so data-driven styling can be used to change the route colors
   * accordingly.
   */
  private static FeatureCollection addTrafficToSource(DirectionsRoute route, int index) {
    final List<Feature> features = new ArrayList<>();
    LineString originalGeometry = LineString.fromPolyline(route.geometry(), Constants.PRECISION_6);
    Feature feat = Feature.fromGeometry(originalGeometry);
    feat.addStringProperty(SOURCE_KEY, String.format(Locale.US, ID_FORMAT, GENERIC_ROUTE_SOURCE_ID,
      index));
    feat.addNumberProperty(INDEX_KEY, index);
    features.add(feat);

    LineString lineString = LineString.fromPolyline(route.geometry(), Constants.PRECISION_6);
    for (RouteLeg leg : route.legs()) {
      if (leg.annotation() != null && leg.annotation().congestion() != null) {
        for (int i = 0; i < leg.annotation().congestion().size(); i++) {
          // See https://github.com/mapbox/mapbox-navigation-android/issues/353
          if (leg.annotation().congestion().size() + 1 <= lineString.getCoordinates().size()) {
            double[] startCoord = lineString.getCoordinates().get(i).getCoordinates();
            double[] endCoord = lineString.getCoordinates().get(i + 1).getCoordinates();

            LineString congestionLineString = LineString.fromCoordinates(new double[][] {startCoord,
              endCoord});
            Feature feature = Feature.fromGeometry(congestionLineString);
            feature.addStringProperty(CONGESTION_KEY, leg.annotation().congestion().get(i));
            feature.addStringProperty(SOURCE_KEY, String.format(Locale.US, ID_FORMAT,
              GENERIC_ROUTE_SOURCE_ID, index));
            feature.addNumberProperty(INDEX_KEY, index);
            features.add(feature);
          }
        }
      } else {
        Feature feature = Feature.fromGeometry(lineString);
        features.add(feature);
      }
    }
    return FeatureCollection.fromFeatures(features);
  }

  /**
   * Takes the directions route list and draws each line on the map.
   */
  private void drawRoutes() {
    // Add all the sources, the list is traversed backwards to ensure the primary route always gets
    // drawn on top of the others since it initially has a index of zero.
    for (int i = featureCollections.size() - 1; i >= 0; i--) {
      MapUtils.updateMapSourceFromFeatureCollection(
        mapboxMap, featureCollections.get(i),
        featureCollections.get(i).getFeatures().get(0).getStringProperty(SOURCE_KEY)
      );

      // Get some required information for the next step
      String sourceId = featureCollections.get(i).getFeatures()
        .get(0).getStringProperty(SOURCE_KEY);
      int index = featureCollections.indexOf(featureCollections.get(i));

      // Add the layer IDs to a list so we can quickly remove them when needed without traversing
      // through all the map layers.
      layerIds.add(String.format(Locale.US, ID_FORMAT, GENERIC_ROUTE_SHIELD_LAYER_ID, index));
      layerIds.add(String.format(Locale.US, ID_FORMAT, GENERIC_ROUTE_LAYER_ID, index));

      // Add the route shield first followed by the route to ensure the shield is always on the
      // bottom.
      addRouteShieldLayer(layerIds.get(layerIds.size() - 2), sourceId, index);
      addRouteLayer(layerIds.get(layerIds.size() - 1), sourceId, index);
    }
  }

  private static Feature getPointFromLineString(RouteLeg leg, int stepIndex) {
    Feature feature = Feature.fromGeometry(Point.fromCoordinates(
      new double[] {
        leg.steps().get(stepIndex).maneuver().location().longitude(),
        leg.steps().get(stepIndex).maneuver().location().latitude()
      }));
    feature.addStringProperty(SOURCE_KEY, WAYPOINT_SOURCE_ID);
    feature.addStringProperty("waypoint",
      stepIndex == 0 ? "origin" : "destination"
    );
    return feature;
  }
}
