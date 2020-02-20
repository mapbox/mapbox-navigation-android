package com.mapbox.services.android.navigation.ui.v5;

import android.location.Location;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.gson.GeometryGeoJson;
import com.mapbox.mapboxsdk.offline.OfflineGeometryRegionDefinition;
import com.mapbox.mapboxsdk.offline.OfflineManager;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import org.junit.Test;
import org.mockito.InOrder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MapOfflineManagerTest {

  @Test
  public void checksDefaultMapConnectivityIsSetWhenDownloadingRouteBuffer() {
    String aRouteSummary = "cjuykbm4705v26pnpvqlbjm5n";
    MapConnectivityController mockedMapConnectivityController = mock(MapConnectivityController.class);
    MapOfflineManager theMapOfflineManager = buildMapOfflineManager(aRouteSummary, mockedMapConnectivityController);
    Location mockedLocation = mock(Location.class);
    Geometry aRouteBufferGeometry = buildARouteBufferGeometry();
    RouteProgress mockedRouteProgress = buildMockRouteProgress(aRouteSummary, aRouteBufferGeometry);
    Boolean defaultState = null;

    theMapOfflineManager.onProgressChange(mockedLocation, mockedRouteProgress);

    verify(mockedMapConnectivityController).assign(eq(defaultState));
  }

  @Test
  public void checksCreateOfflineRegionIsCalledWhenDownloadingRouteBuffer() {
    OfflineManager mockedOfflineManager = mock(OfflineManager.class);
    OfflineRegionDefinitionProvider mockedOfflineRegionDefinitionProvider = mock(OfflineRegionDefinitionProvider.class);
    String guidanceStyleUrl = "mapbox://styles/mapbox/navigation-guidance-day-v4";
    float anyPixelRatio = 3.0f;
    OfflineRegionDefinitionProvider anOfflineRegionDefinitionProvider =
      new OfflineRegionDefinitionProvider(guidanceStyleUrl, anyPixelRatio);
    Geometry aRouteBufferGeometry = buildARouteBufferGeometry();
    OfflineGeometryRegionDefinition routeBufferDefinition =
      anOfflineRegionDefinitionProvider.buildRegionFor(aRouteBufferGeometry);
    when(mockedOfflineRegionDefinitionProvider.buildRegionFor(eq(aRouteBufferGeometry))).thenReturn(routeBufferDefinition);
    OfflineMetadataProvider mockedOfflineMetadataProvider = mock(OfflineMetadataProvider.class);
    OfflineMetadataProvider anOfflineMetadataProvider = new OfflineMetadataProvider();
    String aRouteSummary = "cjuykbm4705v26pnpvqlbjm5n";
    byte[] routeSummaryMetadata = anOfflineMetadataProvider.buildMetadataFor(aRouteSummary);
    when(mockedOfflineMetadataProvider.buildMetadataFor(eq(aRouteSummary))).thenReturn(routeSummaryMetadata);
    MapConnectivityController mockedMapConnectivityController = mock(MapConnectivityController.class);
    RegionDownloadCallback mockedRegionDownloadCallback = mock(RegionDownloadCallback.class);
    MapOfflineManager theMapOfflineManager = new MapOfflineManager(mockedOfflineManager,
      mockedOfflineRegionDefinitionProvider, mockedOfflineMetadataProvider, mockedMapConnectivityController,
      mockedRegionDownloadCallback);
    Location mockedLocation = mock(Location.class);
    RouteProgress mockedRouteProgress = buildMockRouteProgress(aRouteSummary, aRouteBufferGeometry);

    theMapOfflineManager.onProgressChange(mockedLocation, mockedRouteProgress);

    verify(mockedOfflineManager).createOfflineRegion(eq(routeBufferDefinition), eq(routeSummaryMetadata),
      any(CreateOfflineRegionCallback.class));
  }

  @Test
  public void checksDownloadIfPreviousRouteGeometryIsNotNullAndIsNotEqualToCurrentRouteGeometry() {
    OfflineManager mockedOfflineManager = mock(OfflineManager.class);
    OfflineRegionDefinitionProvider mockedOfflineRegionDefinitionProvider = mock(OfflineRegionDefinitionProvider.class);
    String guidanceStyleUrl = "mapbox://styles/mapbox/navigation-guidance-day-v4";
    float anyPixelRatio = 3.0f;
    OfflineRegionDefinitionProvider anOfflineRegionDefinitionProvider =
      new OfflineRegionDefinitionProvider(guidanceStyleUrl, anyPixelRatio);
    OfflineMetadataProvider mockedOfflineMetadataProvider = mock(OfflineMetadataProvider.class);
    OfflineMetadataProvider anOfflineMetadataProvider = new OfflineMetadataProvider();
    String aRouteSummary = "cjuykbm4705v26pnpvqlbjm5n";
    byte[] routeSummaryMetadata = anOfflineMetadataProvider.buildMetadataFor(aRouteSummary);
    when(mockedOfflineMetadataProvider.buildMetadataFor(eq(aRouteSummary))).thenReturn(routeSummaryMetadata);
    MapConnectivityController mockedMapConnectivityController = mock(MapConnectivityController.class);
    RegionDownloadCallback mockedRegionDownloadCallback = mock(RegionDownloadCallback.class);
    MapOfflineManager theMapOfflineManager = new MapOfflineManager(mockedOfflineManager,
      mockedOfflineRegionDefinitionProvider, mockedOfflineMetadataProvider, mockedMapConnectivityController,
      mockedRegionDownloadCallback);
    Location mockedLocation = mock(Location.class);
    Geometry aRouteBufferGeometry = buildARouteBufferGeometry();
    RouteProgress mockedRouteProgress = buildMockRouteProgress(aRouteSummary, aRouteBufferGeometry);
    Geometry anotherRouteBufferGeometry = buildAnotherRouteBufferGeometry();
    when(mockedRouteProgress.routeGeometryWithBuffer()).thenReturn(aRouteBufferGeometry, anotherRouteBufferGeometry);
    OfflineGeometryRegionDefinition aRouteBufferDefinition =
      anOfflineRegionDefinitionProvider.buildRegionFor(aRouteBufferGeometry);
    OfflineGeometryRegionDefinition anotherRouteBufferDefinition =
      anOfflineRegionDefinitionProvider.buildRegionFor(anotherRouteBufferGeometry);
    when(mockedOfflineRegionDefinitionProvider.buildRegionFor(any(Geometry.class)))
      .thenReturn(aRouteBufferDefinition, anotherRouteBufferDefinition);
    Boolean defaultState = null;
    theMapOfflineManager.onProgressChange(mockedLocation, mockedRouteProgress);

    theMapOfflineManager.onProgressChange(mockedLocation, mockedRouteProgress);

    verify(mockedMapConnectivityController, times(2)).assign(eq(defaultState));
    InOrder inOrder = inOrder(mockedOfflineManager, mockedOfflineManager);
    inOrder.verify(mockedOfflineManager).createOfflineRegion(eq(aRouteBufferDefinition), eq(routeSummaryMetadata),
      any(CreateOfflineRegionCallback.class));
    inOrder.verify(mockedOfflineManager).createOfflineRegion(eq(anotherRouteBufferDefinition), eq(routeSummaryMetadata),
      any(CreateOfflineRegionCallback.class));
  }

  @Test
  public void checksMergeOfflineRegionsIsCalledWhenLoadDatabase() {
    OfflineManager mockedOfflineManager = mock(OfflineManager.class);
    OfflineRegionDefinitionProvider mockedOfflineRegionDefinitionProvider = mock(OfflineRegionDefinitionProvider.class);
    OfflineMetadataProvider mockedOfflineMetadataProvider = mock(OfflineMetadataProvider.class);
    MapConnectivityController mockedMapConnectivityController = mock(MapConnectivityController.class);
    RegionDownloadCallback mockedRegionDownloadCallback = mock(RegionDownloadCallback.class);
    MapOfflineManager theMapOfflineManager = new MapOfflineManager(mockedOfflineManager,
      mockedOfflineRegionDefinitionProvider, mockedOfflineMetadataProvider, mockedMapConnectivityController,
      mockedRegionDownloadCallback);
    String aDatabasePath = "a/database/path";
    OfflineDatabaseLoadedCallback mockedOfflineDatabaseLoadedCallback = mock(OfflineDatabaseLoadedCallback.class);

    theMapOfflineManager.loadDatabase(aDatabasePath, mockedOfflineDatabaseLoadedCallback);

    verify(mockedOfflineManager).mergeOfflineRegions(eq(aDatabasePath), any(MergeOfflineRegionsCallback.class));
  }

  @Test
  public void checksMergeOfflineRegionsCallbackOnDestroyIsCalledIfNotNullWhenOnDestroy() {
    OfflineManager mockedOfflineManager = mock(OfflineManager.class);
    OfflineRegionDefinitionProvider mockedOfflineRegionDefinitionProvider = mock(OfflineRegionDefinitionProvider.class);
    OfflineMetadataProvider mockedOfflineMetadataProvider = mock(OfflineMetadataProvider.class);
    MapConnectivityController mockedMapConnectivityController = mock(MapConnectivityController.class);
    RegionDownloadCallback mockedRegionDownloadCallback = mock(RegionDownloadCallback.class);
    MergeOfflineRegionsCallback mockedMergeOfflineRegionsCallback = mock(MergeOfflineRegionsCallback.class);
    MapOfflineManager theMapOfflineManager = new MapOfflineManager(mockedOfflineManager,
      mockedOfflineRegionDefinitionProvider, mockedOfflineMetadataProvider, mockedMapConnectivityController,
      mockedRegionDownloadCallback, mockedMergeOfflineRegionsCallback);

    theMapOfflineManager.onDestroy();

    verify(mockedMergeOfflineRegionsCallback).onDestroy();
  }

  private MapOfflineManager buildMapOfflineManager(String routeSummary,
                                                   MapConnectivityController mapConnectivityController) {
    OfflineManager mockedOfflineManager = mock(OfflineManager.class);
    OfflineRegionDefinitionProvider mockedOfflineRegionDefinitionProvider = mock(OfflineRegionDefinitionProvider.class);
    OfflineMetadataProvider mockedOfflineMetadataProvider = mock(OfflineMetadataProvider.class);
    OfflineMetadataProvider anOfflineMetadataProvider = new OfflineMetadataProvider();
    byte[] routeSummaryMetadata = anOfflineMetadataProvider.buildMetadataFor(routeSummary);
    when(mockedOfflineMetadataProvider.buildMetadataFor(eq(routeSummary))).thenReturn(routeSummaryMetadata);
    RegionDownloadCallback mockedRegionDownloadCallback = mock(RegionDownloadCallback.class);
    return new MapOfflineManager(mockedOfflineManager, mockedOfflineRegionDefinitionProvider,
      mockedOfflineMetadataProvider, mapConnectivityController, mockedRegionDownloadCallback);
  }

  private RouteProgress buildMockRouteProgress(String routeSummary, Geometry routeBufferGeometry) {
    RouteProgress mockedRouteProgress = mock(RouteProgress.class);
    when(mockedRouteProgress.routeGeometryWithBuffer()).thenReturn(routeBufferGeometry);
    DirectionsRoute mockedRoute = mock(DirectionsRoute.class);
    RouteOptions mockedRouteOptions = mock(RouteOptions.class);
    when(mockedRouteOptions.requestUuid()).thenReturn(routeSummary);
    when(mockedRoute.routeOptions()).thenReturn(mockedRouteOptions);
    when(mockedRouteProgress.directionsRoute()).thenReturn(mockedRoute);
    return mockedRouteProgress;
  }

  private Geometry buildARouteBufferGeometry() {
    return GeometryGeoJson.fromJson("{\"type\":\"Polygon\",\"coordinates\":[[[-77" +
      ".152533,39.085537],[-77.152533,39.083038],[-77.150031,39.083038],[-77.150031,39.085537],[-77.147529,39" +
      ".085537],[-77.147529,39.088039],[-77.147529,39.090538],[-77.150031,39.090538],[-77.150031,39.093037],[-77" +
      ".150031,39.095539],[-77.150031,39.098038],[-77.150031,39.100540],[-77.150031,39.103039],[-77.152533,39" +
      ".103039],[-77.152533,39.105537],[-77.155028,39.105537],[-77.155028,39.108040],[-77.155028,39.110538],[-77" +
      ".157531,39.110538],[-77.157531,39.113037],[-77.160033,39.113037],[-77.160033,39.115536],[-77.162528,39" +
      ".115540],[-77.162528,39.118038],[-77.165030,39.118038],[-77.165030,39.115536],[-77.167533,39.115536],[-77" +
      ".167533,39.113037],[-77.167533,39.110538],[-77.165030,39.110538],[-77.165030,39.108040],[-77.162536,39" +
      ".108036],[-77.162536,39.105537],[-77.162536,39.103039],[-77.160033,39.103039],[-77.160033,39.100540],[-77" +
      ".157531,39.100536],[-77.157531,39.098038],[-77.157531,39.095535],[-77.157531,39.093037],[-77.157531,39" +
      ".090538],[-77.157531,39.088039],[-77.155036,39.088036],[-77.155036,39.085537],[-77.152533,39.085537]]]}");
  }

  private Geometry buildAnotherRouteBufferGeometry() {
    return GeometryGeoJson.fromJson("{\"type\":\"Polygon\",\"coordinates\":[[[-77" +
      ".152533,39.085537],[-77.152533,39.083038],[-77.150031,39.083038],[-77.150031,39.085537],[-77.147529,39" +
      ".085537],[-77.147529,39.088039],[-77.147529,39.090538]]]}");
  }
}