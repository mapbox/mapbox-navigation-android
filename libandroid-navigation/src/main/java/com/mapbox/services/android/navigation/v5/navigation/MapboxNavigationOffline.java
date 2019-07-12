package com.mapbox.services.android.navigation.v5.navigation;

import android.support.annotation.NonNull;
import com.mapbox.geojson.BoundingBox;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.navigation.plugin.OfflinePlugin;
import com.mapbox.navigator.Navigator;
import com.mapbox.turf.TurfMeasurement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MapboxNavigationOffline {
    private final List<OfflinePlugin> plugins;
    private final Navigator navigator;

    private MapboxNavigationOffline(@NonNull Builder builder) {
        this.navigator = builder.navigator;
        this.plugins = Collections.unmodifiableList(builder.plugins);
    }

    /**
     * Configures the navigator for offline routes.
     *
     * @param tileSetPath tile set path to configure
     */
    public long configure(@NonNull String tileSetPath) {
        synchronized (this) {
            return navigator.configureRouter(tileSetPath, null, null);
        }
    }

    /**
     * Starts the download of tiles specified by the bounding box generated
     * by the point (center) and apothem passed in.
     *
     * @param point          center of the bounding box
     * @param distanceInKm   distance of the bounding box apothem in km
     * @param tileSetPath    tile set path in which the tile set is stored
     * @param accessToken    a valid Enterprise Mapbox access token
     * @param tileSetVersion version of tile set being requested
     * @param listener       which is updated on error, on progress update and on completion
     */
    public void download(@NonNull Point point, float distanceInKm, @NonNull String tileSetPath,
                         @NonNull String accessToken, @NonNull String tileSetVersion,
                         @NonNull RouteTileDownloadListener listener) {
        BoundingBox boundingBox = generateFrom(point, distanceInKm);
        OfflineTiles tileSet = OfflineTiles.builder()
                .accessToken(accessToken)
                .version(tileSetVersion)
                .boundingBox(boundingBox)
                .build();
        new RouteTileDownloader(new OfflineNavigator(navigator), tileSetPath, listener).startDownload(tileSet);
    }

    private BoundingBox generateFrom(Point center, float apothem) {
        Point northFromCenter = TurfMeasurement.destination(center, apothem, 0, "kilometers");
        Point northeast = TurfMeasurement.destination(northFromCenter, apothem, 90, "kilometers");
        Point southFromCenter = TurfMeasurement.destination(center, apothem, 180, "kilometers");
        Point southwest = TurfMeasurement.destination(southFromCenter, apothem, -90, "kilometers");
        return BoundingBox.fromPoints(southwest, northeast);
    }

    public static final class Builder {
        private final Navigator navigator;
        private final List<OfflinePlugin> plugins = new ArrayList<>();

        Builder(Navigator navigator) {
            this.navigator = navigator;
        }

        public Builder addPlugin(@NonNull OfflinePlugin plugin) {
            if (plugins.contains(plugin)) {
                return this;
            }
            plugins.add(plugin);
            return this;
        }

        public MapboxNavigationOffline install() {
            return new MapboxNavigationOffline(this);
        }
    }
}
