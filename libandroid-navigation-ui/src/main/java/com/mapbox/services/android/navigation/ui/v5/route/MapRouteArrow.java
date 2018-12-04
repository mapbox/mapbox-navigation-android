package com.mapbox.services.android.navigation.ui.v5.route;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.StyleRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.content.res.AppCompatResources;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonOptions;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.mapboxsdk.utils.MathUtils;
import com.mapbox.services.android.navigation.ui.v5.R;
import com.mapbox.services.android.navigation.ui.v5.utils.MapImageUtils;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.turf.TurfConstants;
import com.mapbox.turf.TurfMeasurement;
import com.mapbox.turf.TurfMisc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.mapbox.mapboxsdk.style.expressions.Expression.color;
import static com.mapbox.mapboxsdk.style.expressions.Expression.get;
import static com.mapbox.mapboxsdk.style.expressions.Expression.interpolate;
import static com.mapbox.mapboxsdk.style.expressions.Expression.linear;
import static com.mapbox.mapboxsdk.style.expressions.Expression.step;
import static com.mapbox.mapboxsdk.style.expressions.Expression.stop;
import static com.mapbox.mapboxsdk.style.expressions.Expression.zoom;
import static com.mapbox.mapboxsdk.style.layers.Property.ICON_ROTATION_ALIGNMENT_MAP;
import static com.mapbox.mapboxsdk.style.layers.Property.NONE;
import static com.mapbox.mapboxsdk.style.layers.Property.VISIBLE;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.visibility;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.ARROW_BEARING;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.ARROW_HEAD_CASING_LAYER_ID;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.ARROW_HEAD_CASING_OFFSET;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.ARROW_HEAD_ICON;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.ARROW_HEAD_ICON_CASING;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.ARROW_HEAD_LAYER_ID;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.ARROW_HEAD_OFFSET;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.ARROW_HEAD_SOURCE_ID;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.ARROW_HIDDEN_ZOOM_LEVEL;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.ARROW_SHAFT_CASING_LINE_LAYER_ID;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.ARROW_SHAFT_LINE_LAYER_ID;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.ARROW_SHAFT_SOURCE_ID;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.LAYER_ABOVE_UPCOMING_MANEUVER_ARROW;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.MAX_ARROW_ZOOM;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.MAX_DEGREES;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.MAX_ZOOM_ARROW_HEAD_CASING_SCALE;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.MAX_ZOOM_ARROW_HEAD_SCALE;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.MAX_ZOOM_ARROW_SHAFT_CASING_SCALE;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.MAX_ZOOM_ARROW_SHAFT_SCALE;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.MIN_ARROW_ZOOM;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.MIN_ZOOM_ARROW_HEAD_CASING_SCALE;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.MIN_ZOOM_ARROW_HEAD_SCALE;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.MIN_ZOOM_ARROW_SHAFT_CASING_SCALE;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.MIN_ZOOM_ARROW_SHAFT_SCALE;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.OPAQUE;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.THIRTY;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.TRANSPARENT;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.TWO_POINTS;

class MapRouteArrow {

  @ColorInt
  private final int arrowColor;
  @ColorInt
  private final int arrowBorderColor;

  private List<Layer> arrowLayers;
  private GeoJsonSource arrowShaftGeoJsonSource;
  private GeoJsonSource arrowHeadGeoJsonSource;
  private Feature arrowShaftGeoJsonFeature = Feature.fromGeometry(Point.fromLngLat(0, 0));
  private Feature arrowHeadGeoJsonFeature = Feature.fromGeometry(Point.fromLngLat(0, 0));

  private final MapView mapView;
  private final MapboxMap mapboxMap;

  MapRouteArrow(MapView mapView, MapboxMap mapboxMap, @StyleRes int styleRes) {
    this.mapView = mapView;
    this.mapboxMap = mapboxMap;

    Context context = mapView.getContext();
    TypedArray typedArray = context.obtainStyledAttributes(styleRes, R.styleable.NavigationMapRoute);
    arrowColor = typedArray.getColor(R.styleable.NavigationMapRoute_upcomingManeuverArrowColor,
      ContextCompat.getColor(context, R.color.mapbox_navigation_route_upcoming_maneuver_arrow_color));
    arrowBorderColor = typedArray.getColor(R.styleable.NavigationMapRoute_upcomingManeuverArrowBorderColor,
      ContextCompat.getColor(context, R.color.mapbox_navigation_route_upcoming_maneuver_arrow_border_color));
    typedArray.recycle();

    initialize();
  }

  void addUpcomingManeuverArrow(RouteProgress routeProgress) {
    boolean invalidUpcomingStepPoints = routeProgress.upcomingStepPoints() == null
      || routeProgress.upcomingStepPoints().size() < TWO_POINTS;
    boolean invalidCurrentStepPoints = routeProgress.currentStepPoints().size() < TWO_POINTS;
    if (invalidUpcomingStepPoints || invalidCurrentStepPoints) {
      updateVisibilityTo(false);
      return;
    }
    updateVisibilityTo(true);

    List<Point> maneuverPoints = obtainArrowPointsFrom(routeProgress);
    updateArrowShaftWith(maneuverPoints);
    updateArrowHeadWith(maneuverPoints);
  }

  void updateVisibilityTo(boolean visible) {
    for (Layer layer : arrowLayers) {
      String targetVisibility = visible ? VISIBLE : NONE;
      if (!targetVisibility.equals(layer.getVisibility().getValue())) {
        layer.setProperties(visibility(targetVisibility));
      }
    }
  }

  void onDestroy() {
    for (Layer arrowLayer : arrowLayers) {
      mapboxMap.removeLayer(arrowLayer);
    }
  }

  private List<Point> obtainArrowPointsFrom(RouteProgress routeProgress) {
    List<Point> reversedCurrent = new ArrayList<>(routeProgress.currentStepPoints());
    Collections.reverse(reversedCurrent);

    LineString arrowLineCurrent = LineString.fromLngLats(reversedCurrent);
    LineString arrowLineUpcoming = LineString.fromLngLats(routeProgress.upcomingStepPoints());

    LineString arrowCurrentSliced = TurfMisc.lineSliceAlong(arrowLineCurrent, 0, THIRTY, TurfConstants.UNIT_METERS);
    LineString arrowUpcomingSliced = TurfMisc.lineSliceAlong(arrowLineUpcoming, 0, THIRTY, TurfConstants.UNIT_METERS);

    Collections.reverse(arrowCurrentSliced.coordinates());

    List<Point> combined = new ArrayList<>();
    combined.addAll(arrowCurrentSliced.coordinates());
    combined.addAll(arrowUpcomingSliced.coordinates());
    return combined;
  }

  private void updateArrowShaftWith(List<Point> points) {
    LineString shaft = LineString.fromLngLats(points);
    arrowShaftGeoJsonFeature = Feature.fromGeometry(shaft);
    arrowShaftGeoJsonSource.setGeoJson(arrowShaftGeoJsonFeature);
  }

  private void updateArrowHeadWith(List<Point> points) {
    double azimuth = TurfMeasurement.bearing(points.get(points.size() - 2), points.get(points.size() - 1));
    arrowHeadGeoJsonFeature = Feature.fromGeometry(points.get(points.size() - 1));
    arrowHeadGeoJsonFeature.addNumberProperty(ARROW_BEARING, (float) MathUtils.wrap(azimuth, 0, MAX_DEGREES));
    arrowHeadGeoJsonSource.setGeoJson(arrowHeadGeoJsonFeature);
  }

  private void initialize() {
    initializeArrowShaft();
    initializeArrowHead();

    addArrowHeadIcon();
    addArrowHeadIconCasing();

    LineLayer shaftLayer = createArrowShaftLayer();
    LineLayer shaftCasingLayer = createArrowShaftCasingLayer();
    SymbolLayer headLayer = createArrowHeadLayer();
    SymbolLayer headCasingLayer = createArrowHeadCasingLayer();

    mapboxMap.addLayerBelow(shaftCasingLayer, LAYER_ABOVE_UPCOMING_MANEUVER_ARROW);
    mapboxMap.addLayerAbove(headCasingLayer, shaftCasingLayer.getId());

    mapboxMap.addLayerAbove(shaftLayer, headCasingLayer.getId());
    mapboxMap.addLayerAbove(headLayer, shaftLayer.getId());

    initializeArrowLayers(shaftLayer, shaftCasingLayer, headLayer, headCasingLayer);
  }

  private void initializeArrowShaft() {
    arrowShaftGeoJsonSource = new GeoJsonSource(
      ARROW_SHAFT_SOURCE_ID,
      arrowShaftGeoJsonFeature,
      new GeoJsonOptions().withMaxZoom(16)
    );
    mapboxMap.addSource(arrowShaftGeoJsonSource);
  }

  private void initializeArrowHead() {
    arrowHeadGeoJsonSource = new GeoJsonSource(
      ARROW_HEAD_SOURCE_ID,
      arrowShaftGeoJsonFeature,
      new GeoJsonOptions().withMaxZoom(16)
    );
    mapboxMap.addSource(arrowHeadGeoJsonSource);
  }

  private void addArrowHeadIcon() {
    int headResId = R.drawable.ic_arrow_head;
    Drawable arrowHead = AppCompatResources.getDrawable(mapView.getContext(), headResId);
    if (arrowHead == null) {
      return;
    }
    Drawable head = DrawableCompat.wrap(arrowHead);
    DrawableCompat.setTint(head.mutate(), arrowColor);
    Bitmap icon = MapImageUtils.getBitmapFromDrawable(head);
    mapboxMap.addImage(ARROW_HEAD_ICON, icon);
  }

  private void addArrowHeadIconCasing() {
    int casingResId = R.drawable.ic_arrow_head_casing;
    Drawable arrowHeadCasing = AppCompatResources.getDrawable(mapView.getContext(), casingResId);
    if (arrowHeadCasing == null) {
      return;
    }
    Drawable headCasing = DrawableCompat.wrap(arrowHeadCasing);
    DrawableCompat.setTint(headCasing.mutate(), arrowBorderColor);
    Bitmap icon = MapImageUtils.getBitmapFromDrawable(headCasing);
    mapboxMap.addImage(ARROW_HEAD_ICON_CASING, icon);
  }

  private LineLayer createArrowShaftLayer() {
    LineLayer shaftLayer = (LineLayer) mapboxMap.getLayer(ARROW_SHAFT_LINE_LAYER_ID);
    if (shaftLayer != null) {
      mapboxMap.removeLayer(shaftLayer);
    }
    return new LineLayer(ARROW_SHAFT_LINE_LAYER_ID, ARROW_SHAFT_SOURCE_ID).withProperties(
      PropertyFactory.lineColor(color(arrowColor)),
      PropertyFactory.lineWidth(
        interpolate(linear(), zoom(),
          stop(MIN_ARROW_ZOOM, MIN_ZOOM_ARROW_SHAFT_SCALE),
          stop(MAX_ARROW_ZOOM, MAX_ZOOM_ARROW_SHAFT_SCALE)
        )
      ),
      PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
      PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
      PropertyFactory.visibility(NONE),
      PropertyFactory.lineOpacity(
        step(zoom(), OPAQUE,
          stop(
            ARROW_HIDDEN_ZOOM_LEVEL, TRANSPARENT
          )
        )
      )
    );
  }

  private LineLayer createArrowShaftCasingLayer() {
    LineLayer shaftCasingLayer = (LineLayer) mapboxMap.getLayer(ARROW_SHAFT_CASING_LINE_LAYER_ID);
    if (shaftCasingLayer != null) {
      mapboxMap.removeLayer(shaftCasingLayer);
    }
    return new LineLayer(ARROW_SHAFT_CASING_LINE_LAYER_ID, ARROW_SHAFT_SOURCE_ID).withProperties(
      PropertyFactory.lineColor(color(arrowBorderColor)),
      PropertyFactory.lineWidth(
        interpolate(linear(), zoom(),
          stop(MIN_ARROW_ZOOM, MIN_ZOOM_ARROW_SHAFT_CASING_SCALE),
          stop(MAX_ARROW_ZOOM, MAX_ZOOM_ARROW_SHAFT_CASING_SCALE)
        )
      ),
      PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
      PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
      PropertyFactory.visibility(NONE),
      PropertyFactory.lineOpacity(
        step(zoom(), OPAQUE,
          stop(
            ARROW_HIDDEN_ZOOM_LEVEL, TRANSPARENT
          )
        )
      )
    );
  }

  private SymbolLayer createArrowHeadLayer() {
    SymbolLayer headLayer = (SymbolLayer) mapboxMap.getLayer(ARROW_HEAD_LAYER_ID);
    if (headLayer != null) {
      mapboxMap.removeLayer(headLayer);
    }
    return new SymbolLayer(ARROW_HEAD_LAYER_ID, ARROW_HEAD_SOURCE_ID)
      .withProperties(
        PropertyFactory.iconImage(ARROW_HEAD_ICON),
        iconAllowOverlap(true),
        iconIgnorePlacement(true),
        PropertyFactory.iconSize(interpolate(linear(), zoom(),
          stop(MIN_ARROW_ZOOM, MIN_ZOOM_ARROW_HEAD_SCALE),
          stop(MAX_ARROW_ZOOM, MAX_ZOOM_ARROW_HEAD_SCALE)
          )
        ),
        PropertyFactory.iconOffset(ARROW_HEAD_OFFSET),
        PropertyFactory.iconRotationAlignment(ICON_ROTATION_ALIGNMENT_MAP),
        PropertyFactory.iconRotate(get(ARROW_BEARING)),
        PropertyFactory.visibility(NONE),
        PropertyFactory.iconOpacity(
          step(zoom(), OPAQUE,
            stop(
              ARROW_HIDDEN_ZOOM_LEVEL, TRANSPARENT
            )
          )
        )
      );
  }

  private SymbolLayer createArrowHeadCasingLayer() {
    SymbolLayer headCasingLayer = (SymbolLayer) mapboxMap.getLayer(ARROW_HEAD_CASING_LAYER_ID);
    if (headCasingLayer != null) {
      mapboxMap.removeLayer(headCasingLayer);
    }
    return new SymbolLayer(ARROW_HEAD_CASING_LAYER_ID, ARROW_HEAD_SOURCE_ID).withProperties(
      PropertyFactory.iconImage(ARROW_HEAD_ICON_CASING),
      iconAllowOverlap(true),
      iconIgnorePlacement(true),
      PropertyFactory.iconSize(interpolate(
        linear(), zoom(),
        stop(MIN_ARROW_ZOOM, MIN_ZOOM_ARROW_HEAD_CASING_SCALE),
        stop(MAX_ARROW_ZOOM, MAX_ZOOM_ARROW_HEAD_CASING_SCALE)
      )),
      PropertyFactory.iconOffset(ARROW_HEAD_CASING_OFFSET),
      PropertyFactory.iconRotationAlignment(ICON_ROTATION_ALIGNMENT_MAP),
      PropertyFactory.iconRotate(get(ARROW_BEARING)),
      PropertyFactory.visibility(NONE),
      PropertyFactory.iconOpacity(
        step(zoom(), OPAQUE,
          stop(
            ARROW_HIDDEN_ZOOM_LEVEL, TRANSPARENT
          )
        )
      )
    );
  }

  private void initializeArrowLayers(LineLayer shaftLayer, LineLayer shaftCasingLayer, SymbolLayer headLayer,
                                     SymbolLayer headCasingLayer) {
    arrowLayers = new ArrayList<>();
    arrowLayers.add(shaftCasingLayer);
    arrowLayers.add(shaftLayer);
    arrowLayers.add(headCasingLayer);
    arrowLayers.add(headLayer);
  }
}
