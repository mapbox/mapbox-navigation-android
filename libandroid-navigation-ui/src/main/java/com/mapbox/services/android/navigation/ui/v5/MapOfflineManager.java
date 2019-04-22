package com.mapbox.services.android.navigation.ui.v5;

import android.location.Location;
import android.support.annotation.NonNull;

import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Geometry;
import com.mapbox.mapboxsdk.offline.OfflineGeometryRegionDefinition;
import com.mapbox.mapboxsdk.offline.OfflineManager;
import com.mapbox.mapboxsdk.offline.OfflineRegion;
import com.mapbox.mapboxsdk.offline.OfflineRegionError;
import com.mapbox.mapboxsdk.offline.OfflineRegionStatus;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import timber.log.Timber;

class MapOfflineManager implements ProgressChangeListener {

  private final OfflineManager offlineManager;
  private final OfflineRegionDefinitionProvider definitionProvider;
  private final OfflineMetadataProvider metadataProvider;
  private Geometry currentRouteGeometry;

  MapOfflineManager(OfflineManager offlineManager, OfflineRegionDefinitionProvider definitionProvider,
                    OfflineMetadataProvider metadataProvider) {
    this.offlineManager = offlineManager;
    this.definitionProvider = definitionProvider;
    this.metadataProvider = metadataProvider;
  }

  @Override
  public void onProgressChange(Location location, RouteProgress routeProgress) {
    FeatureCollection routeGeometryWithBuffer = routeProgress.routeGeometryWithBuffer();
    if (currentRouteGeometry == null || !currentRouteGeometry.equals(routeGeometryWithBuffer)) {
      currentRouteGeometry = routeGeometryWithBuffer.features().get(0).geometry();
      // TODO unique identifier for download metadata?
      String routeSummary = routeProgress.directionsRoute().routeOptions().requestUuid();
      download(routeSummary, currentRouteGeometry, new OfflineRegionDownloadCallback() {
        @Override
        public void onComplete() {
          // TODO good to go?
          // TODO Remove debug log after testing
          Timber.d("onComplete!");
        }

        @Override
        public void onError(String error) {
          // TODO fail silently?
          // TODO Remove debug log after testing
          Timber.d("onError %s", error);
        }
      });
    }
  }

  void loadDatabase(@NonNull String offlineDatabasePath, final OfflineDatabaseLoadedCallback callback) {
    offlineManager.mergeOfflineRegions(offlineDatabasePath, new OfflineManager.MergeOfflineRegionsCallback() {
      @Override
      public void onMerge(OfflineRegion[] offlineRegions) {
        callback.onComplete();
      }

      @Override
      public void onError(String error) {
        callback.onError(error);
      }
    });
  }

  private void download(@NonNull String routeSummary, @NonNull Geometry routeGeometry,
                        final OfflineRegionDownloadCallback callback) {
    OfflineGeometryRegionDefinition definition = definitionProvider.buildRegionFor(routeGeometry);
    byte[] metadata = metadataProvider.buildMetaDataFor(routeSummary);
    if (metadata == null) {
      callback.onError("An error occurred processing the offline metadata");
      return;
    }
    offlineManager.createOfflineRegion(definition, metadata, new OfflineManager.CreateOfflineRegionCallback() {
      @Override
      public void onCreate(OfflineRegion offlineRegion) {
        offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE);
        offlineRegion.setObserver(new OfflineRegion.OfflineRegionObserver() {
          @Override
          public void onStatusChanged(OfflineRegionStatus status) {
            if (status.isComplete()) {
              callback.onComplete();
            }
          }

          @Override
          public void onError(OfflineRegionError error) {
            callback.onError(String.format("%s %s", error.getMessage(), error.getReason()));
          }

          @Override
          public void mapboxTileCountLimitExceeded(long limit) {
            callback.onError(String.format("Offline map tile limit reached %s", limit));
          }
        });
      }

      @Override
      public void onError(String error) {
        callback.onError(error);
      }
    });
  }
}