package com.mapbox.services.android.navigation.ui.v5.route;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.content.res.AppCompatResources;

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
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonOptions;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.services.android.navigation.ui.v5.R;
import com.mapbox.services.android.navigation.ui.v5.utils.MapImageUtils;
import com.mapbox.services.android.navigation.ui.v5.utils.MapUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.mapbox.mapboxsdk.style.expressions.Expression.color;
import static com.mapbox.mapboxsdk.style.expressions.Expression.exponential;
import static com.mapbox.mapboxsdk.style.expressions.Expression.get;
import static com.mapbox.mapboxsdk.style.expressions.Expression.interpolate;
import static com.mapbox.mapboxsdk.style.expressions.Expression.literal;
import static com.mapbox.mapboxsdk.style.expressions.Expression.match;
import static com.mapbox.mapboxsdk.style.expressions.Expression.product;
import static com.mapbox.mapboxsdk.style.expressions.Expression.stop;
import static com.mapbox.mapboxsdk.style.expressions.Expression.switchCase;
import static com.mapbox.mapboxsdk.style.expressions.Expression.zoom;
import static com.mapbox.mapboxsdk.style.layers.Property.NONE;
import static com.mapbox.mapboxsdk.style.layers.Property.VISIBLE;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconPitchAlignment;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconSize;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineCap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineJoin;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.visibility;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.DESTINATION_MARKER_NAME;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.HEAVY_CONGESTION_VALUE;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.MODERATE_CONGESTION_VALUE;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.ORIGIN_MARKER_NAME;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.PRIMARY_ROUTE_PROPERTY_KEY;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.ROUTE_LAYER_ID;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.ROUTE_SHIELD_LAYER_ID;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.ROUTE_SOURCE_ID;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.SEVERE_CONGESTION_VALUE;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.WAYPOINT_DESTINATION_VALUE;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.WAYPOINT_LAYER_ID;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.WAYPOINT_ORIGIN_VALUE;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.WAYPOINT_PROPERTY_KEY;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.WAYPOINT_SOURCE_ID;

class MapRouteLine {

  @ColorInt
  private final int routeDefaultColor;
  @ColorInt
  private final int routeModerateColor;
  @ColorInt
  private final int routeSevereColor;
  @ColorInt
  private final int alternativeRouteDefaultColor;
  @ColorInt
  private final int alternativeRouteModerateColor;
  @ColorInt
  private final int alternativeRouteSevereColor;
  @ColorInt
  private final int alternativeRouteShieldColor;
  @ColorInt
  private final int routeShieldColor;
  private final float routeScale;
  private final float alternativeRouteScale;

  private final HashMap<LineString, DirectionsRoute> routeLineStrings = new HashMap<>();
  private final List<FeatureCollection> routeFeatureCollections = new ArrayList<>();
  private final List<DirectionsRoute> directionsRoutes = new ArrayList<>();
  private final MapboxMap mapboxMap;
  private final Drawable originIcon;
  private final Drawable destinationIcon;
  private final GeoJsonSource wayPointSource;
  private final GeoJsonSource routeLineSource;
  private final List<Layer> layers = new ArrayList<>();

  private FeatureCollection wayPointFeatureCollection;
  private String belowLayer;
  private int primaryRouteIndex;
  private boolean alternativesVisible = true;

  MapRouteLine(Context context, MapboxMap mapboxMap, int styleRes, String belowLayer) {
    this.mapboxMap = mapboxMap;
    this.belowLayer = belowLayer;

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

    originIcon = AppCompatResources.getDrawable(context, originWaypointIcon);
    destinationIcon = AppCompatResources.getDrawable(context, destinationWaypointIcon);
    findRouteBelowLayerId();

    GeoJsonOptions wayPointGeoJsonOptions = new GeoJsonOptions().withMaxZoom(16);
    FeatureCollection emptyWayPointFeatureCollection = FeatureCollection.fromFeatures(new Feature[] {});
    wayPointSource = new GeoJsonSource(WAYPOINT_SOURCE_ID, emptyWayPointFeatureCollection, wayPointGeoJsonOptions);
    mapboxMap.addSource(wayPointSource);

    GeoJsonOptions routeLineGeoJsonOptions = new GeoJsonOptions().withMaxZoom(16);
    FeatureCollection emptyRouteLineFeatureCollection = FeatureCollection.fromFeatures(new Feature[] {});
    routeLineSource = new GeoJsonSource(ROUTE_SOURCE_ID, emptyRouteLineFeatureCollection, routeLineGeoJsonOptions);
    mapboxMap.addSource(routeLineSource);

    initializeLayers(mapboxMap);
  }

  void draw(DirectionsRoute directionsRoute) {
    List<DirectionsRoute> route = new ArrayList<>();
    route.add(directionsRoute);
    draw(route);
  }

  void draw(List<DirectionsRoute> directionsRoutes) {
    clearRouteData();
    this.directionsRoutes.addAll(directionsRoutes);
    primaryRouteIndex = 0;
    alternativesVisible = directionsRoutes.size() > 1;
    generateRouteFeatureCollectionsFrom(directionsRoutes);
  }

  void redraw() {
    findRouteBelowLayerId();
    drawRoutes(routeFeatureCollections);
    wayPointSource.setGeoJson(wayPointFeatureCollection);
    toggleAlternativeVisibilityWith(alternativesVisible);
  }

  void toggleAlternativeVisibilityWith(boolean alternativesVisible) {
    this.alternativesVisible = alternativesVisible;
    updateAlternativeVisibilityTo(alternativesVisible);
  }

  void updateVisibilityTo(boolean isVisible) {
    updateAllLayersVisibilityTo(isVisible);
  }

  HashMap<LineString, DirectionsRoute> retrieveRouteLineStrings() {
    return routeLineStrings;
  }

  List<DirectionsRoute> retrieveDirectionsRoutes() {
    return directionsRoutes;
  }

  boolean updatePrimaryRouteIndex(int primaryRouteIndex) {
    boolean isNewIndex = this.primaryRouteIndex != primaryRouteIndex;
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
    updateVisibilityTo(true);
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
    new FeatureProcessingTask(routes, new OnRouteFeaturesProcessedCallback() {
      @Override
      public void onRouteFeaturesProcessed(List<FeatureCollection> routeFeatureCollections,
                                           HashMap<LineString, DirectionsRoute> routeLineStrings) {
        MapRouteLine.this.routeFeatureCollections.addAll(routeFeatureCollections);
        MapRouteLine.this.routeLineStrings.putAll(routeLineStrings);
        drawRoutes(routeFeatureCollections);
        drawWayPoints();
      }
    }).execute();
  }

  private void drawWayPoints() {
    DirectionsRoute primaryRoute = directionsRoutes.get(primaryRouteIndex);
    wayPointFeatureCollection = buildWayPointFeatureCollectionFrom(primaryRoute);
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
    new PrimaryRouteUpdateTask(newPrimaryIndex, routeFeatureCollections, new OnPrimaryRouteUpdatedCallback() {
      @Override
      public void onPrimaryRouteUpdated(List<FeatureCollection> updatedRouteCollections) {
        drawRoutes(updatedRouteCollections);
      }
    }).execute();
  }

  private void findRouteBelowLayerId() {
    if (belowLayer == null || belowLayer.isEmpty()) {
      List<Layer> styleLayers = mapboxMap.getLayers();
      for (int i = 0; i < styleLayers.size(); i++) {
        if (!(styleLayers.get(i) instanceof SymbolLayer)
          // Avoid placing the route on top of the user location layer
          && !styleLayers.get(i).getId().contains(RouteConstants.MAPBOX_LOCATION_ID)) {
          belowLayer = styleLayers.get(i).getId();
        }
      }
    }
  }

  private void initializeLayers(MapboxMap mapboxMap) {
    LineLayer routeShieldLayer = initializeRouteShieldLayer(mapboxMap);
    MapUtils.addLayerToMap(mapboxMap, routeShieldLayer, belowLayer);
    layers.add(routeShieldLayer);

    LineLayer routeLayer = initializeRouteLayer(mapboxMap);
    MapUtils.addLayerToMap(mapboxMap, routeLayer, belowLayer);
    layers.add(routeLayer);

    SymbolLayer wayPointLayer = initializeWayPointLayer(mapboxMap);
    MapUtils.addLayerToMap(mapboxMap, wayPointLayer, belowLayer);
    layers.add(wayPointLayer);
  }

  private LineLayer initializeRouteShieldLayer(MapboxMap mapboxMap) {
    LineLayer shieldLayer = mapboxMap.getLayerAs(ROUTE_SHIELD_LAYER_ID);
    if (shieldLayer != null) {
      mapboxMap.removeLayer(shieldLayer);
    }

    shieldLayer = new LineLayer(ROUTE_SHIELD_LAYER_ID, ROUTE_SOURCE_ID).withProperties(
      lineCap(Property.LINE_CAP_ROUND),
      lineJoin(Property.LINE_JOIN_ROUND),
      lineWidth(
        interpolate(
          exponential(1.5f), zoom(),
          stop(10f, 7f),
          stop(14f, product(literal(10.5f),
            switchCase(
              get(PRIMARY_ROUTE_PROPERTY_KEY), literal(routeScale),
              literal(alternativeRouteScale)))),
          stop(16.5f, product(literal(15.5f),
            switchCase(
              get(PRIMARY_ROUTE_PROPERTY_KEY), literal(routeScale),
              literal(alternativeRouteScale)))),
          stop(19f, product(literal(24f),
            switchCase(
              get(PRIMARY_ROUTE_PROPERTY_KEY), literal(routeScale),
              literal(alternativeRouteScale)))),
          stop(22f, product(literal(29f),
            switchCase(
              get(PRIMARY_ROUTE_PROPERTY_KEY), literal(routeScale),
              literal(alternativeRouteScale))))
        )
      ),
      lineColor(
        switchCase(
          get(PRIMARY_ROUTE_PROPERTY_KEY), color(routeShieldColor),
          color(alternativeRouteShieldColor)
        )
      )
    );
    return shieldLayer;
  }

  private LineLayer initializeRouteLayer(MapboxMap mapboxMap) {
    LineLayer routeLayer = mapboxMap.getLayerAs(ROUTE_LAYER_ID);
    if (routeLayer != null) {
      mapboxMap.removeLayer(routeLayer);
    }

    routeLayer = new LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID).withProperties(
      lineCap(Property.LINE_CAP_ROUND),
      lineJoin(Property.LINE_JOIN_ROUND),
      lineWidth(
        interpolate(
          exponential(1.5f), zoom(),
          stop(4f, product(literal(3f),
            switchCase(
              get(PRIMARY_ROUTE_PROPERTY_KEY), literal(routeScale),
              literal(alternativeRouteScale)))),
          stop(10f, product(literal(4f),
            switchCase(
              get(PRIMARY_ROUTE_PROPERTY_KEY), literal(routeScale),
              literal(alternativeRouteScale)))),
          stop(13f, product(literal(6f),
            switchCase(
              get(PRIMARY_ROUTE_PROPERTY_KEY), literal(routeScale),
              literal(alternativeRouteScale)))),
          stop(16f, product(literal(10f),
            switchCase(
              get(PRIMARY_ROUTE_PROPERTY_KEY), literal(routeScale),
              literal(alternativeRouteScale)))),
          stop(19f, product(literal(14f),
            switchCase(
              get(PRIMARY_ROUTE_PROPERTY_KEY), literal(routeScale),
              literal(alternativeRouteScale)))),
          stop(22f, product(literal(18f),
            switchCase(
              get(PRIMARY_ROUTE_PROPERTY_KEY), literal(routeScale),
              literal(alternativeRouteScale))))
        )
      ),
      lineColor(
        switchCase(
          get(PRIMARY_ROUTE_PROPERTY_KEY), match(
            Expression.toString(get(RouteConstants.CONGESTION_KEY)),
            color(routeDefaultColor),
            stop(MODERATE_CONGESTION_VALUE, color(routeModerateColor)),
            stop(HEAVY_CONGESTION_VALUE, color(routeSevereColor)),
            stop(SEVERE_CONGESTION_VALUE, color(routeSevereColor))
          ),
          match(
            Expression.toString(get(RouteConstants.CONGESTION_KEY)),
            color(alternativeRouteDefaultColor),
            stop(MODERATE_CONGESTION_VALUE, color(alternativeRouteModerateColor)),
            stop(HEAVY_CONGESTION_VALUE, color(alternativeRouteSevereColor)),
            stop(SEVERE_CONGESTION_VALUE, color(alternativeRouteSevereColor))
          )
        )
      )
    );
    return routeLayer;
  }

  private SymbolLayer initializeWayPointLayer(@NonNull MapboxMap mapboxMap) {
    SymbolLayer waypointLayer = mapboxMap.getLayerAs(WAYPOINT_LAYER_ID);
    if (waypointLayer != null) {
      mapboxMap.removeLayer(waypointLayer);
    }

    Bitmap bitmap = MapImageUtils.getBitmapFromDrawable(originIcon);
    mapboxMap.addImage(ORIGIN_MARKER_NAME, bitmap);
    bitmap = MapImageUtils.getBitmapFromDrawable(destinationIcon);
    mapboxMap.addImage(DESTINATION_MARKER_NAME, bitmap);

    waypointLayer = new SymbolLayer(WAYPOINT_LAYER_ID, WAYPOINT_SOURCE_ID).withProperties(
      iconImage(
        match(
          Expression.toString(get(WAYPOINT_PROPERTY_KEY)), literal(ORIGIN_MARKER_NAME),
          stop(WAYPOINT_ORIGIN_VALUE, literal(ORIGIN_MARKER_NAME)),
          stop(WAYPOINT_DESTINATION_VALUE, literal(DESTINATION_MARKER_NAME))
        )),
      iconSize(
        interpolate(
          exponential(1.5f), zoom(),
          stop(0f, 0.6f),
          stop(10f, 0.8f),
          stop(12f, 1.3f),
          stop(22f, 2.8f)
        )
      ),
      iconPitchAlignment(Property.ICON_PITCH_ALIGNMENT_MAP),
      iconAllowOverlap(true),
      iconIgnorePlacement(true)
    );
    return waypointLayer;
  }

  private void updateAlternativeVisibilityTo(boolean isVisible) {
    for (Layer layer : layers) {
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
    for (Layer layer : layers) {
      layer.setProperties(
        visibility(isVisible ? VISIBLE : NONE)
      );
    }
  }

  private void resetSource(GeoJsonSource source) {
    source.setGeoJson(FeatureCollection.fromFeatures(new Feature[] {}));
  }
}
