package com.mapbox.services.android.navigation.ui.v5;

import android.location.Location;
import android.support.annotation.NonNull;

import com.mapbox.geojson.Geometry;
import com.mapbox.mapboxsdk.offline.OfflineGeometryRegionDefinition;
import com.mapbox.mapboxsdk.offline.OfflineManager;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

class MapOfflineManager implements ProgressChangeListener {

  private final OfflineManager offlineManager;
  private final OfflineRegionDefinitionProvider definitionProvider;
  private final OfflineMetadataProvider metadataProvider;
  private final MapConnectivityController connectivityController;
  private final RegionDownloadCallback regionDownloadCallback;
  private Geometry previousRouteGeometry;

  MapOfflineManager(OfflineManager offlineManager, OfflineRegionDefinitionProvider definitionProvider,
                    OfflineMetadataProvider metadataProvider, MapConnectivityController connectivityController,
                    RegionDownloadCallback regionDownloadCallback) {
    this.offlineManager = offlineManager;
    this.definitionProvider = definitionProvider;
    this.metadataProvider = metadataProvider;
    this.connectivityController = connectivityController;
    this.regionDownloadCallback = regionDownloadCallback;
  }

  @Override
  public void onProgressChange(Location location, RouteProgress routeProgress) {
    Geometry currentRouteGeometry = routeProgress.routeGeometryWithBuffer();
    if (previousRouteGeometry == null || !previousRouteGeometry.equals(currentRouteGeometry)) {
      previousRouteGeometry = currentRouteGeometry;
      // TODO unique identifier for download metadata?
      String routeSummary = routeProgress.directionsRoute().routeOptions().requestUuid();
      download(routeSummary, previousRouteGeometry, regionDownloadCallback);
    }
  }

  void loadDatabase(@NonNull String offlineDatabasePath, OfflineDatabaseLoadedCallback callback) {
    offlineManager.mergeOfflineRegions(offlineDatabasePath, new MergeOfflineRegionsCallback(callback));
  }

  private void download(@NonNull String routeSummary, @NonNull Geometry routeGeometry,
                        final OfflineRegionDownloadCallback callback) {
    OfflineGeometryRegionDefinition definition = definitionProvider.buildRegionFor(routeGeometry);
    byte[] metadata = metadataProvider.buildMetadataFor(routeSummary);
    connectivityController.assign(null);
    offlineManager.createOfflineRegion(definition, metadata, new CreateOfflineRegionCallback(callback));
  }
}