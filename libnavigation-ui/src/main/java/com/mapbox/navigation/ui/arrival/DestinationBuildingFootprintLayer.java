package com.mapbox.navigation.ui.arrival;

import android.graphics.Color;
import android.graphics.PointF;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Polygon;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.FillLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.util.List;

import androidx.annotation.NonNull;

import static com.mapbox.mapboxsdk.style.layers.Property.NONE;
import static com.mapbox.mapboxsdk.style.layers.Property.VISIBLE;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillOpacity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.visibility;

public class DestinationBuildingFootprintLayer {

  private static final String DESTINATION_BUILDING_FOOTPRINT_SOURCE_ID = "destination-building-source-id";
  private static final String DESTINATION_BUILDING_FOOTPRINT_LAYER_ID = "destination-building-footprint-layer-id";
  private static final String BUILDING_LAYER_ID = "building";
  private static final String BUILDING_STATION_LAYER_ID = "building station";
  private static final Integer DEFAULT_FOOTPRINT_COLOR = Color.RED;
  private static final Float DEFAULT_BUILDING_FOOTPRINT_OPACITY = 1f;
  private final MapboxMap mapboxMap;
  private final MapView mapView;
  private Polygon buildingPolygon;
  private Feature buildingPolygonFeature;
  private Integer color;
  private Float opacity;

  public DestinationBuildingFootprintLayer(MapboxMap mapboxMap, MapView mapView) {
    this.mapboxMap = mapboxMap;
    this.mapView = mapView;
  }

  /**
   * Toggles the visibility of the destination building highlight layer.
   *
   * @param visible true if the layer should be placed/displayed. False if it should be hidden.
   */
  public void updateVisibility(final boolean visible) {
    if (mapView.isDestroyed()) {
      return;
    }
    mapboxMap.getStyle(new Style.OnStyleLoaded() {
      @Override
      public void onStyleLoaded(@NonNull Style style) {
        FillLayer buildingFootprintFillLayer = style.getLayerAs(DESTINATION_BUILDING_FOOTPRINT_LAYER_ID);
        if (buildingFootprintFillLayer == null) {
          addFootprintHighlightFillLayerToMap();
        } else if ((buildingFootprintFillLayer.getVisibility().value.equals(VISIBLE)) != visible) {
          buildingFootprintFillLayer.setProperties(visibility(NONE));
        }
      }
    });
  }

  /**
   * Set the location of the destination building highlight layer.
   *
   * @param destinationLatLng the new coordinates to use in querying the building layer
   *                          to get the associated {@link Polygon} to eventually highlight.
   */
  public void setDestinationBuildingLocation(final LatLng destinationLatLng) {
    mapboxMap.getStyle(new Style.OnStyleLoaded() {
      @Override
      public void onStyleLoaded(@NonNull Style style) {
        GeoJsonSource geoJsonSource = style.getSourceAs(DESTINATION_BUILDING_FOOTPRINT_SOURCE_ID);
        Polygon polygon = getFootprintPolygonAssociatedWithDestinationBuilding(destinationLatLng);
        if (polygon != null && geoJsonSource != null) {
          geoJsonSource.setGeoJson(polygon);
        }
      }
    });
  }

  /**
   * Set the color of the destination building highlight layer.
   *
   * @param newFootprintColor the new color value
   */
  public void setColor(final int newFootprintColor) {
    mapboxMap.getStyle(new Style.OnStyleLoaded() {
      @Override
      public void onStyleLoaded(@NonNull Style style) {
        FillLayer buildingFootprintFillLayer = style.getLayerAs(DESTINATION_BUILDING_FOOTPRINT_LAYER_ID);
        if (buildingFootprintFillLayer != null) {
          buildingFootprintFillLayer.setProperties(
            fillColor(newFootprintColor));
          color = newFootprintColor;
        }
      }
    });
  }

  /**
   * Set the opacity of the destination building highlight layer.
   *
   * @param newFootprintOpacity the new opacity value
   */
  public void setOpacity(final Float newFootprintOpacity) {
    mapboxMap.getStyle(new Style.OnStyleLoaded() {
      @Override
      public void onStyleLoaded(@NonNull Style style) {
        FillLayer buildingFootprintFillLayer = style.getLayerAs(DESTINATION_BUILDING_FOOTPRINT_LAYER_ID);
        if (buildingFootprintFillLayer != null) {
          buildingFootprintFillLayer.setProperties(
            fillOpacity(opacity));
          opacity = newFootprintOpacity;
        }
      }
    });
  }

  /**
   * Retrieve the {@link Polygon} geometry of the building that the route's final destination
   * coordinates correspond to.
   *
   * @return The {@link Polygon}
   */
  public Polygon getBuildingPolygon() {
    return buildingPolygon;
  }

  /**
   *
   * Retrieve the building {@link Feature} that the route's final destination
   * coordinates correspond to.
   *
   * @return the {@link Feature}
   */
  public Feature getBuildingPolygonFeature() {
    return buildingPolygonFeature;
  }

  /**
   * Retrieve the latest set color of the destination building highlight layer.
   *
   * @return the color Integer
   */
  public Integer getColor() {
    return color;
  }

  /**
   * Retrieve the latest set opacity of the destination building highlight layer.
   *
   * @return the opacity Float
   */
  public Float getOpacity() {
    return opacity;
  }

  private Polygon getFootprintPolygonAssociatedWithDestinationBuilding(final LatLng destinationLatLng) {
    mapboxMap.getStyle(new Style.OnStyleLoaded() {
      @Override
      public void onStyleLoaded(@NonNull Style style) {
        PointF pixel = mapboxMap.getProjection().toScreenLocation(new LatLng(
          destinationLatLng.getLatitude(),
          destinationLatLng.getLongitude()
        ));

        // Check whether the map style has a building layer
        if (style.getLayer(BUILDING_LAYER_ID) != null) {

          // Retrieve the building Feature that is displayed in the middle of the map
          List<Feature> features = mapboxMap.queryRenderedFeatures(pixel, BUILDING_LAYER_ID, BUILDING_STATION_LAYER_ID);
          if (features.size() > 0) {
            if (features.get(0).geometry() instanceof Polygon) {
              buildingPolygonFeature = features.get(0);
              buildingPolygon = (Polygon) features.get(0).geometry();
            }
          }
        }
      }
    });
    return buildingPolygon;
  }

  private void addFootprintHighlightFillLayerToMap() {
    mapboxMap.getStyle(new Style.OnStyleLoaded() {
      @Override
      public void onStyleLoaded(@NonNull Style style) {
        FillLayer existingBuildingLayerId = style.getLayerAs(BUILDING_LAYER_ID);
        if (existingBuildingLayerId != null) {
          GeoJsonSource buildingFootprintGeojsonSource = new GeoJsonSource(DESTINATION_BUILDING_FOOTPRINT_SOURCE_ID);
          style.addSource(buildingFootprintGeojsonSource);
          FillLayer finalDestinationBuildingFillLayer = new FillLayer(DESTINATION_BUILDING_FOOTPRINT_LAYER_ID,
            DESTINATION_BUILDING_FOOTPRINT_SOURCE_ID);
          finalDestinationBuildingFillLayer.setProperties(
            fillColor(color == null ? DEFAULT_FOOTPRINT_COLOR :
              color),
            fillOpacity(opacity == null ? DEFAULT_BUILDING_FOOTPRINT_OPACITY :
              opacity)
          );
          style.addLayerAbove(finalDestinationBuildingFillLayer, BUILDING_LAYER_ID);
        }
      }
    });
  }
}
