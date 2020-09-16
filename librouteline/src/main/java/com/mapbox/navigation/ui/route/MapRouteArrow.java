package com.mapbox.navigation.ui.route;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import com.mapbox.bindgen.Expected;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
//import com.mapbox.mapboxsdk.maps.MapView;
//import com.mapbox.mapboxsdk.maps.MapboxMap;
//import com.mapbox.mapboxsdk.maps.Style;
//import com.mapbox.mapboxsdk.style.layers.*;
//import com.mapbox.mapboxsdk.style.sources.GeoJsonOptions;
//import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
//import com.mapbox.mapboxsdk.utils.MathUtils;
import com.mapbox.maps.MapView;
import com.mapbox.maps.MapboxMap;
import com.mapbox.maps.Style;
import com.mapbox.maps.plugin.style.expressions.Expression;
import com.mapbox.maps.plugin.style.layers.Layer;
import com.mapbox.maps.plugin.style.layers.LineLayer;
import com.mapbox.maps.plugin.style.layers.SymbolLayer;
import com.mapbox.maps.plugin.style.layers.properties.IconRotationAlignment;
import com.mapbox.maps.plugin.style.layers.properties.Visibility;
import com.mapbox.maps.plugin.style.sources.GeojsonSource;
import com.mapbox.navigation.base.trip.model.RouteProgress;
import com.mapbox.navigation.ui.R;
import com.mapbox.navigation.ui.internal.utils.MapImageUtils;
import com.mapbox.turf.TurfConstants;
import com.mapbox.turf.TurfMeasurement;
import com.mapbox.turf.TurfMisc;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

//import static com.mapbox.mapboxsdk.style.expressions.Expression.*;
//import static com.mapbox.mapboxsdk.style.layers.Property.*;
//import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.*;
import static com.mapbox.navigation.ui.internal.route.RouteConstants.*;
import static com.mapbox.navigation.ui.internal.utils.CompareUtils.areEqualContentsIgnoreOrder;

class MapRouteArrow {

  @ColorInt
  private final int arrowColor;
  @ColorInt
  private final int arrowBorderColor;

  private List<Point> maneuverPoints;
  private List<String> arrowLayerIds;
  private GeojsonSource arrowShaftGeoJsonSource;
  private GeojsonSource arrowHeadGeoJsonSource;

  @NonNull
  private final MapView mapView;
  private final MapboxMap mapboxMap;
  private boolean isVisible = true;

  MapRouteArrow(@NonNull MapView mapView, MapboxMap mapboxMap, @StyleRes int styleRes, @NonNull String aboveLayer) {
    this.mapView = mapView;
    this.mapboxMap = mapboxMap;
    this.maneuverPoints = new ArrayList<>();

    Context context = mapView.getContext();
    TypedArray typedArray = context.obtainStyledAttributes(styleRes, R.styleable.MapboxStyleNavigationMapRoute);
    arrowColor = typedArray.getColor(R.styleable.MapboxStyleNavigationMapRoute_upcomingManeuverArrowColor,
            ContextCompat.getColor(context, R.color.mapbox_navigation_route_upcoming_maneuver_arrow_color));
    arrowBorderColor = typedArray.getColor(R.styleable.MapboxStyleNavigationMapRoute_upcomingManeuverArrowBorderColor,
            ContextCompat.getColor(context, R.color.mapbox_navigation_route_upcoming_maneuver_arrow_border_color));
    typedArray.recycle();

    initialize(aboveLayer);
    updateVisibilityTo(isVisible);
  }

  void addUpcomingManeuverArrow(@NonNull RouteProgress routeProgress) {
    boolean invalidUpcomingStepPoints = routeProgress.getUpcomingStepPoints() == null
            || routeProgress.getUpcomingStepPoints().size() < TWO_POINTS;
    boolean invalidCurrentStepPoints = routeProgress.getCurrentLegProgress() == null
            || routeProgress.getCurrentLegProgress().getCurrentStepProgress() == null
            || routeProgress.getCurrentLegProgress().getCurrentStepProgress().getStepPoints() == null
            || routeProgress.getCurrentLegProgress().getCurrentStepProgress().getStepPoints().size() < TWO_POINTS;
    if (invalidUpcomingStepPoints || invalidCurrentStepPoints) {
      updateVisibilityTo(false);
      return;
    }
    updateVisibilityTo(true);

    List<Point> newManeuverPoints = obtainArrowPointsFrom(routeProgress);
    if (!areEqualContentsIgnoreOrder(maneuverPoints, newManeuverPoints)) {
      maneuverPoints.clear();
      maneuverPoints.addAll(newManeuverPoints);
      updateArrowShaftWith(maneuverPoints);
      updateArrowHeadWith(maneuverPoints);
    }
  }

  void updateVisibilityTo(boolean visible) {
    this.isVisible = visible;
    mapboxMap.getStyle(style -> {
      for (String layerId : arrowLayerIds) {
        if (style.layerExists(layerId)) {
          Expected expectedLayer = style.getLayerProperties(layerId);
          if (expectedLayer.isValue()) {
            Layer layer = (Layer)expectedLayer.getValue();
            Visibility targetVisibility = visible ? Visibility.VISIBLE : Visibility.NONE;
            if (targetVisibility != layer.getVisibility()) {
              layer.visibility(targetVisibility);
            }
          }
        }
      }
    });
  }

  public boolean routeArrowIsVisible() {
    return isVisible;
  }

  @NonNull
  private List<Point> obtainArrowPointsFrom(@NonNull RouteProgress routeProgress) {
    List<Point> reversedCurrent =
      new ArrayList<>(routeProgress.getCurrentLegProgress().getCurrentStepProgress().getStepPoints());
    Collections.reverse(reversedCurrent);

    LineString arrowLineCurrent = LineString.fromLngLats(reversedCurrent);
    LineString arrowLineUpcoming = LineString.fromLngLats(routeProgress.getUpcomingStepPoints());

    LineString arrowCurrentSliced =
      TurfMisc.lineSliceAlong(arrowLineCurrent, 0, THIRTY, TurfConstants.UNIT_METERS);
    LineString arrowUpcomingSliced =
      TurfMisc.lineSliceAlong(arrowLineUpcoming, 0, THIRTY, TurfConstants.UNIT_METERS);

    Collections.reverse(arrowCurrentSliced.coordinates());

    List<Point> combined = new ArrayList<>();
    combined.addAll(arrowCurrentSliced.coordinates());
    combined.addAll(arrowUpcomingSliced.coordinates());
    return combined;
  }

  private void updateArrowShaftWith(@NonNull List<Point> points) {
    LineString shaft = LineString.fromLngLats(points);
    Feature arrowShaftGeoJsonFeature = Feature.fromGeometry(shaft);

    //fixme arrowShaftGeoJsonSource.updateGeoJSON(arrowShaftGeoJsonFeature);
  }

  private void updateArrowHeadWith(@NonNull List<Point> points) {
    double azimuth = TurfMeasurement.bearing(points.get(points.size() - 2), points.get(points.size() - 1));
    Feature arrowHeadGeoJsonFeature = Feature.fromGeometry(points.get(points.size() - 1));
    //arrowHeadGeoJsonFeature.addNumberProperty(ARROW_BEARING, (float) MathUtils.wrap(azimuth, 0, MAX_DEGREES));
    //fixme arrowHeadGeoJsonSource.setGeoJson(arrowHeadGeoJsonFeature);
  }

  private void initialize(@NonNull String aboveLayer) {
    initializeArrowShaft();
    initializeArrowHead();

    //addArrowHeadIcon();
    //addArrowHeadIconCasing();

    //LineLayer shaftLayer = createArrowShaftLayer();
    //LineLayer shaftCasingLayer = createArrowShaftCasingLayer();
    //SymbolLayer headLayer = createArrowHeadLayer();
    //SymbolLayer headCasingLayer = createArrowHeadCasingLayer();

    //mapboxMap.getStyle().addLayerAbove(shaftCasingLayer, aboveLayer);
    //mapboxMap.getStyle().addLayerAbove(headCasingLayer, shaftCasingLayer.getId());
    //
    //mapboxMap.getStyle().addLayerAbove(shaftLayer, headCasingLayer.getId());
    //mapboxMap.getStyle().addLayerAbove(headLayer, shaftLayer.getId());

    //createArrowLayerList(shaftLayer, shaftCasingLayer, headLayer, headCasingLayer);
  }

  private void initializeArrowShaft() {
    arrowShaftGeoJsonSource = new GeojsonSource.Builder(ARROW_SHAFT_SOURCE_ID)
        .featureCollection(FeatureCollection.fromFeatures(new Feature[]{}))
        .maxzoom(16)
        .build();

    mapboxMap.getStyle(style -> {
      arrowShaftGeoJsonSource.bindTo(style);
    });
  }

  private void initializeArrowHead() {
    arrowHeadGeoJsonSource = new GeojsonSource.Builder(ARROW_HEAD_SOURCE_ID)
        .featureCollection(FeatureCollection.fromFeatures(new Feature[]{}))
        .maxzoom(16)
        .build();

    mapboxMap.getStyle(style -> {
      arrowHeadGeoJsonSource.bindTo(style);
    });
  }

  //private void addArrowHeadIcon() {
  //  int headResId = R.drawable.mapbox_ic_arrow_head;
  //  Drawable arrowHead = AppCompatResources.getDrawable(mapView.getContext(), headResId);
  //  if (arrowHead == null) {
  //    return;
  //  }
  //  Drawable head = DrawableCompat.wrap(arrowHead);
  //  DrawableCompat.setTint(head.mutate(), arrowColor);
  //  Bitmap icon = MapImageUtils.getBitmapFromDrawable(head);
  //  mapboxMap.getStyle().addImage(ARROW_HEAD_ICON, icon);
  //}
  //
  //private void addArrowHeadIconCasing() {
  //  int casingResId = R.drawable.mapbox_ic_arrow_head_casing;
  //  Drawable arrowHeadCasing = AppCompatResources.getDrawable(mapView.getContext(), casingResId);
  //  if (arrowHeadCasing == null) {
  //    return;
  //  }
  //  Drawable headCasing = DrawableCompat.wrap(arrowHeadCasing);
  //  DrawableCompat.setTint(headCasing.mutate(), arrowBorderColor);
  //  Bitmap icon = MapImageUtils.getBitmapFromDrawable(headCasing);
  //  mapboxMap.getStyle().addImage(ARROW_HEAD_ICON_CASING, icon);
  //}

  //@NonNull
  //private LineLayer createArrowShaftLayer() {
  //  LineLayer shaftLayer = (LineLayer) mapboxMap.getStyle().getLayer(ARROW_SHAFT_LINE_LAYER_ID);
  //  if (shaftLayer != null) {
  //    mapboxMap.getStyle().removeLayer(shaftLayer);
  //  }
  //  return new LineLayer(ARROW_SHAFT_LINE_LAYER_ID, ARROW_SHAFT_SOURCE_ID).withProperties(
  //          PropertyFactory.lineColor(color(arrowColor)),
  //          PropertyFactory.lineWidth(
  //                  interpolate(linear(), zoom(),
  //                          stop(MIN_ARROW_ZOOM, MIN_ZOOM_ARROW_SHAFT_SCALE),
  //                          stop(MAX_ARROW_ZOOM, MAX_ZOOM_ARROW_SHAFT_SCALE)
  //                  )
  //          ),
  //          PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
  //          PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
  //          PropertyFactory.visibility(NONE),
  //          PropertyFactory.lineOpacity(
  //                  step(zoom(), OPAQUE,
  //                          stop(
  //                                  ARROW_HIDDEN_ZOOM_LEVEL, TRANSPARENT
  //                          )
  //                  )
  //          )
  //  );
  //}

  //@NonNull
  //private LineLayer createArrowShaftCasingLayer() {
  //  LineLayer shaftCasingLayer = (LineLayer) mapboxMap.getStyle().getLayer(ARROW_SHAFT_CASING_LINE_LAYER_ID);
  //  if (shaftCasingLayer != null) {
  //    mapboxMap.getStyle().removeLayer(shaftCasingLayer);
  //  }
  //  return new LineLayer(ARROW_SHAFT_CASING_LINE_LAYER_ID, ARROW_SHAFT_SOURCE_ID).withProperties(
  //          PropertyFactory.lineColor(color(arrowBorderColor)),
  //          PropertyFactory.lineWidth(
  //                  interpolate(linear(), zoom(),
  //                          stop(MIN_ARROW_ZOOM, MIN_ZOOM_ARROW_SHAFT_CASING_SCALE),
  //                          stop(MAX_ARROW_ZOOM, MAX_ZOOM_ARROW_SHAFT_CASING_SCALE)
  //                  )
  //          ),
  //          PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
  //          PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
  //          PropertyFactory.visibility(NONE),
  //          PropertyFactory.lineOpacity(
  //                  step(zoom(), OPAQUE,
  //                          stop(
  //                                  ARROW_HIDDEN_ZOOM_LEVEL, TRANSPARENT
  //                          )
  //                  )
  //          )
  //  );
  //}

  //@NonNull
  //private SymbolLayer createArrowHeadLayer() {
  //  SymbolLayer headLayer = (SymbolLayer) mapboxMap.getStyle().getLayer(ARROW_HEAD_LAYER_ID);
  //  if (headLayer != null) {
  //    mapboxMap.getStyle().removeLayer(headLayer);
  //  }
  //  return new SymbolLayer(ARROW_HEAD_LAYER_ID, ARROW_HEAD_SOURCE_ID)
  //          .withProperties(
  //                  PropertyFactory.iconImage(ARROW_HEAD_ICON),
  //                  iconAllowOverlap(true),
  //                  iconIgnorePlacement(true),
  //                  PropertyFactory.iconSize(interpolate(linear(), zoom(),
  //                          stop(MIN_ARROW_ZOOM, MIN_ZOOM_ARROW_HEAD_SCALE),
  //                          stop(MAX_ARROW_ZOOM, MAX_ZOOM_ARROW_HEAD_SCALE)
  //                          )
  //                  ),
  //                  PropertyFactory.iconOffset(ARROW_HEAD_OFFSET),
  //                  PropertyFactory.iconRotationAlignment(ICON_ROTATION_ALIGNMENT_MAP),
  //                  PropertyFactory.iconRotate(get(ARROW_BEARING)),
  //                  PropertyFactory.visibility(NONE),
  //                  PropertyFactory.iconOpacity(
  //                          step(zoom(), OPAQUE,
  //                                  stop(
  //                                          ARROW_HIDDEN_ZOOM_LEVEL, TRANSPARENT
  //                                  )
  //                          )
  //                  )
  //          );
  //}

  @NonNull
  private SymbolLayer createArrowHeadCasingLayer() {
    return null;

    //mapboxMap.getStyle(style -> {
    //  if (style.layerExists(ARROW_HEAD_CASING_LAYER_ID)) {
    //    style.removeLayer(ARROW_HEAD_CASING_LAYER_ID);
    //  }
    //
    //  Expression.ExpressionBuilder iconSizeExpressionBuilder = new Expression.ExpressionBuilder("interpolate");
    //  iconSizeExpressionBuilder.literal("linear");
    //  iconSizeExpressionBuilder.zoom();
    //  iconSizeExpressionBuilder.stop(expressionBuilderRef -> {
    //    expressionBuilderRef.literal(MIN_ARROW_ZOOM);
    //    expressionBuilderRef.literal(MIN_ZOOM_ARROW_HEAD_CASING_SCALE);
    //    return null;
    //  });
    //  iconSizeExpressionBuilder.stop(expressionBuilderRef -> {
    //    expressionBuilderRef.literal(MAX_ARROW_ZOOM);
    //    expressionBuilderRef.literal(MAX_ZOOM_ARROW_HEAD_CASING_SCALE);
    //    return null;
    //  });
    //
    //  Expression.ExpressionBuilder iconRotateExpressionBuilder = new Expression.ExpressionBuilder("get");
    //  iconRotateExpressionBuilder.literal(ARROW_BEARING);
    //
    //  Expression.ExpressionBuilder iconOpacityExpressionBuilder = new Expression.ExpressionBuilder("step");
    //  iconOpacityExpressionBuilder.zoom();
    //  iconOpacityExpressionBuilder.stop(expressionBuilder -> {
    //    expressionBuilder.literal(ARROW_HIDDEN_ZOOM_LEVEL);
    //    expressionBuilder.literal(TRANSPARENT);
    //    return null;
    //  });
    //
    //  SymbolLayer casingLayer = new SymbolLayer(ARROW_HEAD_CASING_LAYER_ID, ARROW_HEAD_SOURCE_ID);
    //  casingLayer.iconImage(ARROW_HEAD_ICON_CASING);
    //  casingLayer.iconAllowOverlap(true);
    //  casingLayer.iconSize(iconSizeExpressionBuilder.build());
    //  casingLayer.iconOffset(Arrays.asList(ARROW_HEAD_CASING_OFFSET));
    //  casingLayer.iconRotationAlignment(IconRotationAlignment.MAP);
    //  casingLayer.iconRotate(iconRotateExpressionBuilder.build());
    //  casingLayer.visibility(Visibility.NONE);
    //  casingLayer.iconOpacity(iconOpacityExpressionBuilder.build()); //["step", ["zoom"], 0.0, 14.0, 1.0]
    //});



    //SymbolLayer headCasingLayer = (SymbolLayer) mapboxMap.getStyle().getLayer(ARROW_HEAD_CASING_LAYER_ID);
    //if (headCasingLayer != null) {
    //  mapboxMap.getStyle().removeLayer(headCasingLayer);
    //}
    //return new SymbolLayer(ARROW_HEAD_CASING_LAYER_ID, ARROW_HEAD_SOURCE_ID).withProperties(
    //        PropertyFactory.iconImage(ARROW_HEAD_ICON_CASING),
    //        iconAllowOverlap(true),
    //        iconIgnorePlacement(true),
    //        PropertyFactory.iconSize(interpolate(
    //                linear(), zoom(),
    //                stop(MIN_ARROW_ZOOM, MIN_ZOOM_ARROW_HEAD_CASING_SCALE),
    //                stop(MAX_ARROW_ZOOM, MAX_ZOOM_ARROW_HEAD_CASING_SCALE)
    //        )),
    //        PropertyFactory.iconOffset(ARROW_HEAD_CASING_OFFSET),
    //        PropertyFactory.iconRotationAlignment(ICON_ROTATION_ALIGNMENT_MAP),
    //        PropertyFactory.iconRotate(get(ARROW_BEARING)),
    //        PropertyFactory.visibility(NONE),
    //        PropertyFactory.iconOpacity(
    //                step(zoom(), OPAQUE,
    //                        stop(
    //                                ARROW_HIDDEN_ZOOM_LEVEL, TRANSPARENT
    //                        )
    //                )
    //        )
    //);
  }

  private void createArrowLayerList(
          @NonNull LineLayer shaftLayer,
          @NonNull LineLayer shaftCasingLayer,
          @NonNull SymbolLayer headLayer,
          @NonNull SymbolLayer headCasingLayer) {
    arrowLayerIds = new ArrayList<>();
    arrowLayerIds.add(shaftCasingLayer.getLayerId());
    arrowLayerIds.add(shaftLayer.getLayerId());
    arrowLayerIds.add(headCasingLayer.getLayerId());
    arrowLayerIds.add(headLayer.getLayerId());
  }
}
