package com.mapbox.services.android.navigation.v5.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonOptions;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

/**
 * Utils class useful for performing map operations such as adding sources, layers, and more.
 *
 * @since 0.8.0
 */
public final class MapUtils {

    private MapUtils() {
        // Hide constructor to prevent initialization
    }

    /**
     * Takes a {@link FeatureCollection} and creates a map GeoJson source using the sourceId also
     * provided.
     *
     * @param mapboxMap  that the current mapView is using
     * @param collection the feature collection to be added to the map style
     * @param sourceId   the source's id for identifying it when adding layers
     * @since 0.8.0
     */
    public static void updateMapSourceFromFeatureCollection(@NonNull MapboxMap mapboxMap,
                                                            @Nullable FeatureCollection collection,
                                                            @NonNull String sourceId) {
        if (collection == null) {
            collection = FeatureCollection.fromFeatures(new Feature[]{});
        }

        if (mapboxMap.getStyle() != null) {
            GeoJsonSource source = mapboxMap.getStyle().getSourceAs(sourceId);
            if (source == null) {
                GeoJsonOptions routeGeoJsonOptions = new GeoJsonOptions().withMaxZoom(16);
                GeoJsonSource routeSource = new GeoJsonSource(sourceId, collection, routeGeoJsonOptions);
                mapboxMap.getStyle().addSource(routeSource);
            } else {
                source.setGeoJson(collection);
            }
        }
    }

    /**
     * Generic method for adding layers to the map.
     *
     * @param mapboxMap    that the current mapView is using
     * @param layer        a layer that will be added to the map
     * @param idBelowLayer optionally providing the layer which the new layer should be placed below
     * @since 0.8.0
     */
    public static void addLayerToMap(@NonNull MapboxMap mapboxMap, @NonNull Layer layer,
                                     @Nullable String idBelowLayer) {
        try {
            if (mapboxMap.getStyle().getLayer(layer.getId()) != null) {
                return;
            }
            if (idBelowLayer == null) {
                mapboxMap.getStyle().addLayer(layer);
            } else {
                mapboxMap.getStyle().addLayerBelow(layer, idBelowLayer);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
