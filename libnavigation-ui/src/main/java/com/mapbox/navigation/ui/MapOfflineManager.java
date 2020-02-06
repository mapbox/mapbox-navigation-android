package com.mapbox.navigation.ui;

import androidx.annotation.NonNull;

import com.mapbox.geojson.Geometry;
import com.mapbox.mapboxsdk.offline.OfflineGeometryRegionDefinition;
import com.mapbox.mapboxsdk.offline.OfflineManager;
import com.mapbox.navigation.base.trip.model.RouteProgress;
import com.mapbox.navigation.core.trip.session.RouteProgressObserver;

import org.jetbrains.annotations.NotNull;

class MapOfflineManager implements RouteProgressObserver {

  private final OfflineManager offlineManager;
  private final OfflineRegionDefinitionProvider definitionProvider;
  private final OfflineMetadataProvider metadataProvider;
  private final MapConnectivityController connectivityController;
  private final RegionDownloadCallback regionDownloadCallback;
  private Geometry previousRouteGeometry;
  private MergeOfflineRegionsCallback mergeOfflineRegionsCallback;

  MapOfflineManager(OfflineManager offlineManager, OfflineRegionDefinitionProvider definitionProvider,
                    OfflineMetadataProvider metadataProvider, MapConnectivityController connectivityController,
                    RegionDownloadCallback regionDownloadCallback) {
    this.offlineManager = offlineManager;
    this.definitionProvider = definitionProvider;
    this.metadataProvider = metadataProvider;
    this.connectivityController = connectivityController;
    this.regionDownloadCallback = regionDownloadCallback;
  }

  // Package private (no modifier) for testing purposes
  MapOfflineManager(OfflineManager offlineManager, OfflineRegionDefinitionProvider definitionProvider,
                    OfflineMetadataProvider metadataProvider, MapConnectivityController connectivityController,
                    RegionDownloadCallback regionDownloadCallback,
                    MergeOfflineRegionsCallback mergeOfflineRegionsCallback) {
    this.offlineManager = offlineManager;
    this.definitionProvider = definitionProvider;
    this.metadataProvider = metadataProvider;
    this.connectivityController = connectivityController;
    this.regionDownloadCallback = regionDownloadCallback;
    this.mergeOfflineRegionsCallback = mergeOfflineRegionsCallback;
  }

  @Override
  public void onRouteProgressChanged(@NotNull RouteProgress routeProgress) {
    Geometry currentRouteGeometry = routeProgress.routeGeometryWithBuffer();
    if (previousRouteGeometry == null || !previousRouteGeometry.equals(currentRouteGeometry)) {
      previousRouteGeometry = currentRouteGeometry;
      String routeSummary = routeProgress.route().routeOptions().requestUuid();
      download(routeSummary, previousRouteGeometry, regionDownloadCallback);
    }
  }

  void loadDatabase(@NonNull String offlineDatabasePath, OfflineDatabaseLoadedCallback callback) {
    mergeOfflineRegionsCallback = new MergeOfflineRegionsCallback(callback);
    offlineManager.mergeOfflineRegions(offlineDatabasePath, mergeOfflineRegionsCallback);
  }

  void onDestroy() {
    if (mergeOfflineRegionsCallback != null) {
      mergeOfflineRegionsCallback.onDestroy();
    }
  }

  private void download(@NonNull String routeSummary, @NonNull Geometry routeGeometry,
                        final OfflineRegionDownloadCallback callback) {
    OfflineGeometryRegionDefinition definition = definitionProvider.buildRegionFor(routeGeometry);
    byte[] metadata = metadataProvider.buildMetadataFor(routeSummary);
    connectivityController.assign(null);
    offlineManager.createOfflineRegion(definition, metadata, new CreateOfflineRegionCallback(callback));
  }
}