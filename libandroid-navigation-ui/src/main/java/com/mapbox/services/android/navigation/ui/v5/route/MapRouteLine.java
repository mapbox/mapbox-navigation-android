package com.mapbox.services.android.navigation.ui.v5.route;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.core.content.ContextCompat;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.style.expressions.Expression;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonOptions;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.services.android.navigation.ui.v5.R;
import com.mapbox.services.android.navigation.ui.v5.utils.MapUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.mapbox.mapboxsdk.style.expressions.Expression.literal;
import static com.mapbox.mapboxsdk.style.layers.Property.NONE;
import static com.mapbox.mapboxsdk.style.layers.Property.VISIBLE;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.visibility;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.PRIMARY_ROUTE_PROPERTY_KEY;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.ROUTE_LAYER_ID;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.ROUTE_SHIELD_LAYER_ID;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.ROUTE_SOURCE_ID;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.WAYPOINT_DESTINATION_VALUE;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.WAYPOINT_ORIGIN_VALUE;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.WAYPOINT_PROPERTY_KEY;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.WAYPOINT_SOURCE_ID;

class MapRouteLine {

  @ColorInt
  private int routeDefaultColor;
  @ColorInt
  private int routeModerateColor;
  @ColorInt
  private int routeSevereColor;
  @ColorInt
  private int alternativeRouteDefaultColor;
  @ColorInt
  private int alternativeRouteModerateColor;
  @ColorInt
  private int alternativeRouteSevereColor;
  @ColorInt
  private int alternativeRouteShieldColor;
  @ColorInt
  private int routeShieldColor;
  private float routeScale;
  private float alternativeRouteScale;
  private boolean roundedLineCap;

  private final HashMap<LineString, DirectionsRoute> routeLineStrings = new HashMap<>();
  private final List<FeatureCollection> routeFeatureCollections = new ArrayList<>();
  private final List<DirectionsRoute> directionsRoutes = new ArrayList<>();
  private final List<Layer> routeLayers;

  private MapboxMap mapboxMap;
  private Drawable originIcon;
  private Drawable destinationIcon;
  private GeoJsonSource wayPointSource;
  private GeoJsonSource routeLineSource;
  private String belowLayer;
  private int primaryRouteIndex;
  private boolean isVisible = true;
  private boolean alternativesVisible = true;

  MapRouteLine(Context context,
               MapboxMap mapboxMap,
               int styleRes,
               String belowLayer,
               MapRouteDrawableProvider drawableProvider,
               MapRouteSourceProvider sourceProvider,
               MapRouteLayerProvider layerProvider) {
    this.mapboxMap = mapboxMap;
    this.belowLayer = belowLayer;
    this.routeLayers = new ArrayList<>();

    TypedArray typedArray = context.obtainStyledAttributes(styleRes, R.styleable.NavigationMapRoute);
    // Primary Route attributes
    routeDefaultColor = typedArray.getColor(R.styleable.NavigationMapRoute_routeColor,
      ContextCompat.getColor(context, R.color.mapbox_navigation_route_layer_blue));
    routeModerateColor = typedArray.getColor(
      R.styleable.NavigationMapRoute_routeModerateCongestionColor,
      ContextCompat.getColor(context, R.color.mapbox_navigation_route_layer_congestion_yellow));
    routeSevereColor = typedArray.getColor(
      R.styleable.NavigationMapRoute_routeSevereCongestionColor,
      ContextCompat.getColor(context, R.color.mapbox_navigation_route_layer_congestion_red));
    routeShieldColor = typedArray.getColor(R.styleable.NavigationMapRoute_routeShieldColor,
      ContextCompat.getColor(context, R.color.mapbox_navigation_route_shield_layer_color));
    routeScale = typedArray.getFloat(R.styleable.NavigationMapRoute_routeScale, 1.0f);
    roundedLineCap = typedArray.getBoolean(R.styleable.NavigationMapRoute_roundedLineCap, true);

    // Secondary Routes attributes
    alternativeRouteDefaultColor = typedArray.getColor(
      R.styleable.NavigationMapRoute_alternativeRouteColor,
      ContextCompat.getColor(context, R.color.mapbox_navigation_route_alternative_color));
    alternativeRouteModerateColor = typedArray.getColor(
      R.styleable.NavigationMapRoute_alternativeRouteModerateCongestionColor,
      ContextCompat.getColor(context, R.color.mapbox_navigation_route_alternative_congestion_yellow));
    alternativeRouteSevereColor = typedArray.getColor(
      R.styleable.NavigationMapRoute_alternativeRouteSevereCongestionColor,
      ContextCompat.getColor(context, R.color.mapbox_navigation_route_alternative_congestion_red));
    alternativeRouteShieldColor = typedArray.getColor(
      R.styleable.NavigationMapRoute_alternativeRouteShieldColor,
      ContextCompat.getColor(context, R.color.mapbox_navigation_route_alternative_shield_color));
    alternativeRouteScale = typedArray.getFloat(
      R.styleable.NavigationMapRoute_alternativeRouteScale, 1.0f);

    // Waypoint attributes
    int originWaypointIcon = typedArray.getResourceId(
      R.styleable.NavigationMapRoute_originWaypointIcon, R.drawable.ic_route_origin);
    int destinationWaypointIcon = typedArray.getResourceId(
      R.styleable.NavigationMapRoute_destinationWaypointIcon, R.drawable.ic_route_destination);
    typedArray.recycle();

    originIcon = drawableProvider.retrieveDrawable(originWaypointIcon);
    destinationIcon = drawableProvider.retrieveDrawable(destinationWaypointIcon);
    findRouteBelowLayerId();

    GeoJsonOptions wayPointGeoJsonOptions = new GeoJsonOptions().withMaxZoom(16);
    FeatureCollection emptyWayPointFeatureCollection = FeatureCollection.fromFeatures(new Feature[] {});
    wayPointSource = sourceProvider.build(WAYPOINT_SOURCE_ID, emptyWayPointFeatureCollection, wayPointGeoJsonOptions);
    mapboxMap.getStyle().addSource(wayPointSource);

    GeoJsonOptions routeLineGeoJsonOptions = new GeoJsonOptions().withMaxZoom(16);
    FeatureCollection emptyRouteLineFeatureCollection = FeatureCollection.fromFeatures(new Feature[] {});
    routeLineSource = sourceProvider.build(ROUTE_SOURCE_ID, emptyRouteLineFeatureCollection, routeLineGeoJsonOptions);
    mapboxMap.getStyle().addSource(routeLineSource);

    initializeLayers(mapboxMap, layerProvider);
  }

  // For testing only
  MapRouteLine(GeoJsonSource routeLineSource, GeoJsonSource wayPointSource, List<Layer> routeLayers) {
    this.routeLineSource = routeLineSource;
    this.wayPointSource = wayPointSource;
    this.routeLayers = routeLayers;
  }

  void draw(DirectionsRoute directionsRoute) {
    List<DirectionsRoute> route = new ArrayList<>();
    route.add(directionsRoute);
    draw(route);
  }

  void draw(List<DirectionsRoute> directionsRoutes) {
    if (directionsRoutes.isEmpty()) {
      return;
    }
    clearRouteData();
    this.directionsRoutes.addAll(directionsRoutes);
    primaryRouteIndex = 0;
    alternativesVisible = directionsRoutes.size() > 1;
    isVisible = true;
    generateRouteFeatureCollectionsFrom(directionsRoutes);
  }

  void redraw(List<DirectionsRoute> routes, boolean alternativesVisible,
              int primaryRouteIndex, boolean isVisible) {
    draw(routes);
    this.alternativesVisible = alternativesVisible;
    this.primaryRouteIndex = primaryRouteIndex;
    this.isVisible = isVisible;
  }

  void toggleAlternativeVisibilityWith(boolean alternativesVisible) {
    this.alternativesVisible = alternativesVisible;
    updateAlternativeVisibilityTo(alternativesVisible);
  }

  boolean retrieveAlternativesVisible() {
    return alternativesVisible;
  }

  void updateVisibilityTo(boolean isVisible) {
    updateAllLayersVisibilityTo(isVisible);
  }

  boolean retrieveVisibility() {
    return isVisible;
  }

  HashMap<LineString, DirectionsRoute> retrieveRouteLineStrings() {
    return routeLineStrings;
  }

  List<DirectionsRoute> retrieveDirectionsRoutes() {
    return directionsRoutes;
  }

  boolean updatePrimaryRouteIndex(int primaryRouteIndex) {
    boolean isNewIndex = this.primaryRouteIndex != primaryRouteIndex
      && primaryRouteIndex < directionsRoutes.size() && primaryRouteIndex >= 0;
    if (isNewIndex) {
      this.primaryRouteIndex = primaryRouteIndex;
      updateRoutesFor(primaryRouteIndex);
    }
    return isNewIndex;
  }

  int retrievePrimaryRouteIndex() {
    return primaryRouteIndex;
  }

  private void drawRoutes(List<FeatureCollection> routeFeatureCollections) {
    List<Feature> routeFeatures = new ArrayList<>();
    for (int i = routeFeatureCollections.size() - 1; i >= 0; i--) {
      routeFeatures.addAll(routeFeatureCollections.get(i).features());
    }
    routeLineSource.setGeoJson(FeatureCollection.fromFeatures(routeFeatures));
  }

  private void clearRouteData() {
    clearRouteListData();
    resetSource(wayPointSource);
    resetSource(routeLineSource);
  }

  private void clearRouteListData() {
    if (!directionsRoutes.isEmpty()) {
      directionsRoutes.clear();
    }
    if (!routeLineStrings.isEmpty()) {
      routeLineStrings.clear();
    }
    if (!routeFeatureCollections.isEmpty()) {
      routeFeatureCollections.clear();
    }
  }

  private void generateRouteFeatureCollectionsFrom(List<DirectionsRoute> routes) {
    new FeatureProcessingTask(routes, routeFeaturesProcessedCallback).execute();
  }

  private OnRouteFeaturesProcessedCallback routeFeaturesProcessedCallback = new OnRouteFeaturesProcessedCallback() {
    @Override
    public void onRouteFeaturesProcessed(List<FeatureCollection> routeFeatureCollections,
                                         HashMap<LineString, DirectionsRoute> routeLineStrings) {
      MapRouteLine.this.routeFeatureCollections.addAll(routeFeatureCollections);
      MapRouteLine.this.routeLineStrings.putAll(routeLineStrings);
      drawRoutes(routeFeatureCollections);
      drawWayPoints();
      updateAlternativeVisibilityTo(alternativesVisible);
      updateRoutesFor(primaryRouteIndex);
      updateVisibilityTo(isVisible);
    }
  };

  private void drawWayPoints() {
    DirectionsRoute primaryRoute = directionsRoutes.get(primaryRouteIndex);
    FeatureCollection wayPointFeatureCollection = buildWayPointFeatureCollectionFrom(primaryRoute);
    wayPointSource.setGeoJson(wayPointFeatureCollection);
  }

  private FeatureCollection buildWayPointFeatureCollectionFrom(DirectionsRoute route) {
    final List<Feature> wayPointFeatures = new ArrayList<>();
    for (RouteLeg leg : route.legs()) {
      wayPointFeatures.add(buildWayPointFeatureFromLeg(leg, 0));
      wayPointFeatures.add(buildWayPointFeatureFromLeg(leg, leg.steps().size() - 1));
    }
    return FeatureCollection.fromFeatures(wayPointFeatures);
  }

  private Feature buildWayPointFeatureFromLeg(RouteLeg leg, int index) {
    Feature feature = Feature.fromGeometry(Point.fromLngLat(
      leg.steps().get(index).maneuver().location().longitude(),
      leg.steps().get(index).maneuver().location().latitude()
    ));
    feature.addStringProperty(WAYPOINT_PROPERTY_KEY, index == 0 ? WAYPOINT_ORIGIN_VALUE : WAYPOINT_DESTINATION_VALUE);
    return feature;
  }

  private void updateRoutesFor(int newPrimaryIndex) {
    if (newPrimaryIndex < 0 || newPrimaryIndex > routeFeatureCollections.size() - 1) {
      return;
    }
    new PrimaryRouteUpdateTask(newPrimaryIndex, routeFeatureCollections, primaryRouteUpdatedCallback).execute();
  }

  private OnPrimaryRouteUpdatedCallback primaryRouteUpdatedCallback = new OnPrimaryRouteUpdatedCallback() {
    @Override
    public void onPrimaryRouteUpdated(List<FeatureCollection> updatedRouteCollections) {
      drawRoutes(updatedRouteCollections);
    }
  };

  private void findRouteBelowLayerId() {
    if (belowLayer == null || belowLayer.isEmpty()) {
      List<Layer> styleLayers = mapboxMap.getStyle().getLayers();
      for (int i = 0; i < styleLayers.size(); i++) {
        if (!(styleLayers.get(i) instanceof SymbolLayer)
          // Avoid placing the route on top of the user location layer
          && !styleLayers.get(i).getId().contains(RouteConstants.MAPBOX_LOCATION_ID)) {
          belowLayer = styleLayers.get(i).getId();
        }
      }
    }
  }

  private void initializeLayers(MapboxMap mapboxMap, MapRouteLayerProvider layerProvider) {
    LineLayer routeShieldLayer = layerProvider.initializeRouteShieldLayer(
      mapboxMap, routeScale, alternativeRouteScale,
      routeShieldColor, alternativeRouteShieldColor
    );
    MapUtils.addLayerToMap(mapboxMap, routeShieldLayer, belowLayer);
    routeLayers.add(routeShieldLayer);

    LineLayer routeLayer = layerProvider.initializeRouteLayer(
      mapboxMap, roundedLineCap, routeScale, alternativeRouteScale,
      routeDefaultColor, routeModerateColor, routeSevereColor,
      alternativeRouteDefaultColor, alternativeRouteModerateColor,
      alternativeRouteSevereColor
    );
    MapUtils.addLayerToMap(mapboxMap, routeLayer, belowLayer);
    routeLayers.add(routeLayer);

    SymbolLayer wayPointLayer = layerProvider.initializeWayPointLayer(
      mapboxMap, originIcon, destinationIcon
    );
    MapUtils.addLayerToMap(mapboxMap, wayPointLayer, belowLayer);
    routeLayers.add(wayPointLayer);
  }

  private void updateAlternativeVisibilityTo(boolean isVisible) {
    for (Layer layer : routeLayers) {
      String layerId = layer.getId();
      if (layerId.equals(ROUTE_LAYER_ID) || layerId.equals(ROUTE_SHIELD_LAYER_ID)) {
        LineLayer route = (LineLayer) layer;
        if (isVisible) {
          route.setFilter(literal(true));
        } else {
          route.setFilter(Expression.eq(Expression.get(PRIMARY_ROUTE_PROPERTY_KEY), true));
        }
      }
    }
  }

  private void updateAllLayersVisibilityTo(boolean isVisible) {
    this.isVisible = isVisible;
    for (Layer layer : routeLayers) {
      layer.setProperties(
        visibility(isVisible ? VISIBLE : NONE)
      );
    }
  }

  private void resetSource(GeoJsonSource source) {
    source.setGeoJson(FeatureCollection.fromFeatures(new Feature[] {}));
  }
}
