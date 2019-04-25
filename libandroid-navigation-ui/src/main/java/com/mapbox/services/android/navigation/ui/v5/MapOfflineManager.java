package com.mapbox.services.android.navigation.ui.v5;

import android.location.Location;
import android.support.annotation.NonNull;

import com.mapbox.geojson.Geometry;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.offline.OfflineGeometryRegionDefinition;
import com.mapbox.mapboxsdk.offline.OfflineManager;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

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
    Geometry routeGeometry = routeProgress.routeGeometryWithBuffer();
    if (currentRouteGeometry == null || !currentRouteGeometry.equals(routeGeometry)) {
      currentRouteGeometry = routeGeometry;
      // TODO unique identifier for download metadata?
      String routeSummary = routeProgress.directionsRoute().routeOptions().requestUuid();
      download(routeSummary, currentRouteGeometry, new RegionDownloadCallback());
    }
  }

  void loadDatabase(@NonNull String offlineDatabasePath, final OfflineDatabaseLoadedCallback callback) {
    offlineManager.mergeOfflineRegions(offlineDatabasePath, new MergeOfflineRegionsCallback(callback));
  }

  private void download(@NonNull String routeSummary, @NonNull Geometry routeGeometry,
                        final OfflineRegionDownloadCallback callback) {
    OfflineGeometryRegionDefinition definition = definitionProvider.buildRegionFor(routeGeometry);
    byte[] metadata = metadataProvider.buildMetaDataFor(routeSummary);
    if (metadata == null) {
      callback.onError("An error occurred processing the offline metadata");
      return;
    }
    Mapbox.setConnected(null);
    offlineManager.createOfflineRegion(definition, metadata, new CreateOfflineRegionCallback(callback));
  }
}