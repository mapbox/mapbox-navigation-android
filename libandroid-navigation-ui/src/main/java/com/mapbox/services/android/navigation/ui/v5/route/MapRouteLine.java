package com.mapbox.services.android.navigation.ui.v5.route;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Handler;

import androidx.annotation.ColorInt;
import androidx.core.content.ContextCompat;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.maps.Style;
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
import java.util.concurrent.atomic.AtomicReference;

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

  private Style style;

  private final HashMap<LineString, DirectionsRoute> routeLineStrings = new HashMap<>();
  private final List<FeatureCollection> routeFeatureCollections = new ArrayList<>();
  private final List<DirectionsRoute> directionsRoutes = new ArrayList<>();
  private final List<String> routeLayerIds;

  private final GeoJsonSource wayPointSource;
  private final GeoJsonSource routeLineSource;
  private int primaryRouteIndex;
  private boolean isVisible = true;
  private boolean alternativesVisible = true;
  private FeatureCollection drawnRouteFeatureCollection;
  private FeatureCollection drawnWaypointsFeatureCollection;
  private AtomicReference<FeatureProcessingTask> featureProcessingTaskRef = new AtomicReference<>(null);
  private FeatureProcessingTask featureProcessingTask;
  private boolean isFeatureProcessingTaskInjected = false;
  private AtomicReference<PrimaryRouteUpdateTask> primaryRouteUpdateTaskRef = new AtomicReference<>(null);
  private PrimaryRouteUpdateTask primaryRouteUpdateTask;
  private boolean isPrimaryRouteUpdateTaskInjected = false;
  private Handler mainHandler;

  MapRouteLine(Context context,
               Style style,
               int styleRes,
               String belowLayer,
               MapRouteDrawableProvider drawableProvider,
               MapRouteSourceProvider sourceProvider,
               MapRouteLayerProvider layerProvider,
               Handler handler) {
    this(context, style, styleRes, belowLayer, drawableProvider, sourceProvider, layerProvider,
      FeatureCollection.fromFeatures(new Feature[]{}),
      FeatureCollection.fromFeatures(new Feature[]{}),
      new ArrayList<DirectionsRoute>(),
      new ArrayList<FeatureCollection>(),
      new HashMap<LineString, DirectionsRoute>(),
      0,
      true,
      true,
      handler);
  }

  MapRouteLine(Context context,
               Style style,
               int styleRes,
               String belowLayer,
               MapRouteDrawableProvider drawableProvider,
               MapRouteSourceProvider sourceProvider,
               MapRouteLayerProvider layerProvider,
               FeatureCollection routesFeatureCollection,
               FeatureCollection waypointsFeatureCollection,
               List<DirectionsRoute> directionsRoutes,
               List<FeatureCollection> routeFeatureCollections,
               HashMap<LineString, DirectionsRoute> routeLineStrings,
               int primaryRouteIndex,
               boolean isVisible,
               boolean alternativesVisible,
               Handler handler
  ) {
    this.routeLayerIds = new ArrayList<>();
    this.mainHandler = handler;
    this.style = style;

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

    GeoJsonOptions wayPointGeoJsonOptions = new GeoJsonOptions().withMaxZoom(16);
    drawnWaypointsFeatureCollection = waypointsFeatureCollection;
    wayPointSource = sourceProvider.build(WAYPOINT_SOURCE_ID, drawnWaypointsFeatureCollection, wayPointGeoJsonOptions);
    style.addSource(wayPointSource);

    GeoJsonOptions routeLineGeoJsonOptions = new GeoJsonOptions().withMaxZoom(16);
    drawnRouteFeatureCollection = routesFeatureCollection;
    routeLineSource = sourceProvider.build(ROUTE_SOURCE_ID, drawnRouteFeatureCollection, routeLineGeoJsonOptions);
    style.addSource(routeLineSource);

    // Waypoint attributes
    int originWaypointIcon = typedArray.getResourceId(
      R.styleable.NavigationMapRoute_originWaypointIcon, R.drawable.ic_route_origin);
    int destinationWaypointIcon = typedArray.getResourceId(
      R.styleable.NavigationMapRoute_destinationWaypointIcon, R.drawable.ic_route_destination);
    typedArray.recycle();

    Drawable originIcon = drawableProvider.retrieveDrawable(originWaypointIcon);
    Drawable destinationIcon = drawableProvider.retrieveDrawable(destinationWaypointIcon);
    belowLayer = findRouteBelowLayerId(belowLayer, style);

    initializeLayers(style, layerProvider, originIcon, destinationIcon, belowLayer);

    this.directionsRoutes.addAll(directionsRoutes);
    this.routeFeatureCollections.addAll(routeFeatureCollections);
    this.routeLineStrings.putAll(routeLineStrings);

    updateAlternativeVisibilityTo(alternativesVisible);
    updateRoutesFor(primaryRouteIndex);
    updateVisibilityTo(isVisible);
  }

  // For testing only
  MapRouteLine(GeoJsonSource routeLineSource, GeoJsonSource wayPointSource, List<String> routeLayerIds) {
    this.routeLineSource = routeLineSource;
    this.wayPointSource = wayPointSource;
    this.routeLayerIds = routeLayerIds;
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

  List<FeatureCollection> retrieveRouteFeatureCollections() {
    return routeFeatureCollections;
  }

  FeatureCollection retrieveDrawnRouteFeatureCollections() {
    return drawnRouteFeatureCollection;
  }

  FeatureCollection retrieveDrawnWaypointsFeatureCollections() {
    return drawnWaypointsFeatureCollection;
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
    setRoutesSource(FeatureCollection.fromFeatures(routeFeatures));
  }

  private void clearRouteData() {
    clearRouteListData();
    setRoutesSource(FeatureCollection.fromFeatures(new Feature[]{}));
    setWaypointsSource(FeatureCollection.fromFeatures(new Feature[]{}));
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
    // Retrieve a possibly null task. The retrieve is atomic.
    FeatureProcessingTask task = featureProcessingTaskRef.getAndSet(retrieveFeatureProcessingTask(routes));
    // If the previous task is valid, cancel it.
    if (task != null) {
      task.cancel();
    }
    // Retrieve the newly created task again. Maybe null
    task = featureProcessingTaskRef.get();

    // If the new task is not null, start it
    if (task != null) {
      task.start();
    }
  }

  // Testing only
  void injectFeatureProcessingTask(FeatureProcessingTask featureProcessingTask) {
    this.isFeatureProcessingTaskInjected = true;
    this.featureProcessingTask = featureProcessingTask;
  }

  private FeatureProcessingTask retrieveFeatureProcessingTask(List<DirectionsRoute> routes) {
    if (isFeatureProcessingTaskInjected) {
      return featureProcessingTask;
    }
    return new FeatureProcessingTask(routes, routeFeaturesProcessedCallback, mainHandler);
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

  // Testing only
  OnRouteFeaturesProcessedCallback retrieveRouteFeaturesProcessedCallback() {
    return routeFeaturesProcessedCallback;
  }

  private void drawWayPoints() {
    DirectionsRoute primaryRoute = directionsRoutes.get(primaryRouteIndex);
    setWaypointsSource(buildWayPointFeatureCollectionFrom(primaryRoute));
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
    this.primaryRouteIndex = newPrimaryIndex;
    if (newPrimaryIndex < 0 || newPrimaryIndex > routeFeatureCollections.size() - 1) {
      return;
    }
    PrimaryRouteUpdateTask task = primaryRouteUpdateTaskRef.getAndSet(retrievePrimaryRouteUpdateTask(newPrimaryIndex));
    if (task != null) {
      task.cancel();
    }

    // Retrieve the newly created task again. Maybe null
    task = primaryRouteUpdateTaskRef.get();

    // If the new task is not null, start it
    if (task != null) {
      task.start();
    }
  }

  // Testing only
  void injectPrimaryRouteUpdateTask(PrimaryRouteUpdateTask primaryRouteUpdateTask) {
    this.isPrimaryRouteUpdateTaskInjected = true;
    this.primaryRouteUpdateTask = primaryRouteUpdateTask;
  }

  private PrimaryRouteUpdateTask retrievePrimaryRouteUpdateTask(int newPrimaryIndex) {
    if (isPrimaryRouteUpdateTaskInjected) {
      return primaryRouteUpdateTask;
    }
    return new PrimaryRouteUpdateTask(newPrimaryIndex,
      routeFeatureCollections, primaryRouteUpdatedCallback, mainHandler);
  }

  private OnPrimaryRouteUpdatedCallback primaryRouteUpdatedCallback = new OnPrimaryRouteUpdatedCallback() {
    @Override
    public void onPrimaryRouteUpdated(List<FeatureCollection> updatedRouteCollections) {
      drawRoutes(updatedRouteCollections);
    }
  };

  // Testing only
  OnPrimaryRouteUpdatedCallback retrievePrimaryRouteUpdatedCallback() {
    return primaryRouteUpdatedCallback;
  }

  private String findRouteBelowLayerId(String belowLayer, Style style) {
    if (belowLayer == null || belowLayer.isEmpty()) {
      List<Layer> styleLayers = style.getLayers();
      for (int i = 0; i < styleLayers.size(); i++) {
        if (!(styleLayers.get(i) instanceof SymbolLayer)
          // Avoid placing the route on top of the user location layer
          && !styleLayers.get(i).getId().contains(RouteConstants.MAPBOX_LOCATION_ID)) {
          belowLayer = styleLayers.get(i).getId();
        }
      }
    }
    return belowLayer;
  }

  private void initializeLayers(Style style, MapRouteLayerProvider layerProvider,
                                Drawable originIcon, Drawable destinationIcon,
                                String belowLayer) {
    LineLayer routeShieldLayer = layerProvider.initializeRouteShieldLayer(
      style, routeScale, alternativeRouteScale,
      routeShieldColor, alternativeRouteShieldColor
    );
    MapUtils.addLayerToMap(style, routeShieldLayer, belowLayer);
    routeLayerIds.add(routeShieldLayer.getId());

    LineLayer routeLayer = layerProvider.initializeRouteLayer(
      style, roundedLineCap, routeScale, alternativeRouteScale,
      routeDefaultColor, routeModerateColor, routeSevereColor,
      alternativeRouteDefaultColor, alternativeRouteModerateColor,
      alternativeRouteSevereColor
    );
    MapUtils.addLayerToMap(style, routeLayer, belowLayer);
    routeLayerIds.add(routeLayer.getId());

    SymbolLayer wayPointLayer = layerProvider.initializeWayPointLayer(
      style, originIcon, destinationIcon
    );
    MapUtils.addLayerToMap(style, wayPointLayer, belowLayer);
    routeLayerIds.add(wayPointLayer.getId());
  }

  private void updateAlternativeVisibilityTo(boolean isAlternativeVisible) {
    this.alternativesVisible = isAlternativeVisible;
    if (style != null && style.isFullyLoaded()) {
      for (String layerId : routeLayerIds) {
        if (layerId.equals(ROUTE_LAYER_ID) || layerId.equals(ROUTE_SHIELD_LAYER_ID)) {
          Layer layer = style.getLayer(layerId);
          if (layer != null) {
            LineLayer route = (LineLayer) layer;
            if (isAlternativeVisible) {
              route.setFilter(literal(true));
            } else {
              route.setFilter(Expression.eq(Expression.get(PRIMARY_ROUTE_PROPERTY_KEY), true));
            }
          }
        }
      }
    }
  }

  private void updateAllLayersVisibilityTo(boolean isVisible) {
    this.isVisible = isVisible;
    if (style != null && style.isFullyLoaded()) {
      for (String layerId : routeLayerIds) {
        Layer layer = style.getLayer(layerId);
        if (layer != null) {
          layer.setProperties(
            visibility(isVisible ? VISIBLE : NONE)
          );
        }
      }
    }
  }

  private void setRoutesSource(FeatureCollection featureCollection) {
    drawnRouteFeatureCollection = featureCollection;
    routeLineSource.setGeoJson(drawnRouteFeatureCollection);
  }

  private void setWaypointsSource(FeatureCollection featureCollection) {
    drawnWaypointsFeatureCollection = featureCollection;
    wayPointSource.setGeoJson(drawnWaypointsFeatureCollection);
  }
}
