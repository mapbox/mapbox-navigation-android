package com.mapbox.navigation.ui.map;

import android.location.Location;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationUpdate;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Projection;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.mapboxsdk.style.sources.Source;
import com.mapbox.mapboxsdk.style.sources.VectorSource;
import com.mapbox.navigation.core.MapboxNavigation;
import com.mapbox.navigation.core.trip.session.MapMatcherResult;
import com.mapbox.navigation.core.trip.session.MapMatcherResultObserver;
import com.mapbox.navigation.ui.camera.NavigationCamera;
import com.mapbox.navigation.ui.camera.SimpleCamera;
import com.mapbox.navigation.ui.route.NavigationMapRoute;
import com.mapbox.navigation.ui.route.OnRouteSelectionChangeListener;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NavigationMapboxMapTest {

  @Test
  public void updateIncidentsVisibility_layerIdentifierIsClosures() {
    MapLayerInteractor mockedMapLayerInteractor = mock(MapLayerInteractor.class);
    NavigationMapboxMap theNavigationMap = new NavigationMapboxMap(mockedMapLayerInteractor);
    boolean anyVisibility = true;

    theNavigationMap.updateIncidentsVisibility(anyVisibility);

    verify(mockedMapLayerInteractor).updateLayerVisibility(eq(anyVisibility), eq("closures"));
  }

  @Test
  public void isIncidentsVisible_layerIdentifierIsClosures() {
    MapLayerInteractor mockedMapLayerInteractor = mock(MapLayerInteractor.class);
    NavigationMapboxMap theNavigationMap = new NavigationMapboxMap(mockedMapLayerInteractor);

    theNavigationMap.isIncidentsVisible();

    verify(mockedMapLayerInteractor).isLayerVisible(eq("closures"));
  }

  @Test
  public void updateTrafficVisibility_layerIdentifierIsTraffic() {
    MapLayerInteractor mockedMapLayerInteractor = mock(MapLayerInteractor.class);
    NavigationMapboxMap theNavigationMap = new NavigationMapboxMap(mockedMapLayerInteractor);
    boolean anyVisibility = false;

    theNavigationMap.updateTrafficVisibility(anyVisibility);

    verify(mockedMapLayerInteractor).updateLayerVisibility(eq(anyVisibility), eq("traffic"));
  }

  @Test
  public void isTrafficVisible_layerIdentifierIsTraffic() {
    MapLayerInteractor mockedMapLayerInteractor = mock(MapLayerInteractor.class);
    NavigationMapboxMap theNavigationMap = new NavigationMapboxMap(mockedMapLayerInteractor);

    theNavigationMap.isTrafficVisible();

    verify(mockedMapLayerInteractor).isLayerVisible(eq("traffic"));
  }

  @Test
  public void updateRenderMode_locationComponentIsUpdatedWithRenderMode() {
    LocationComponent locationComponent = mock(LocationComponent.class);
    NavigationMapboxMap theNavigationMap = new NavigationMapboxMap(locationComponent);
    int renderMode = RenderMode.GPS;

    theNavigationMap.updateLocationLayerRenderMode(renderMode);

    verify(locationComponent).setRenderMode(eq(renderMode));
  }

  @Test
  public void drawRoutes_mapRoutesAreAdded() {
    NavigationMapRoute mapRoute = mock(NavigationMapRoute.class);
    NavigationMapboxMap theNavigationMap = new NavigationMapboxMap(mapRoute);
    List<DirectionsRoute> routes = new ArrayList<>();

    theNavigationMap.drawRoutes(routes);

    verify(mapRoute).addRoutes(eq(routes));
  }

  @Test
  public void setOnRouteSelectionChangeListener_listenerIsSet() {
    NavigationMapRoute mapRoute = mock(NavigationMapRoute.class);
    NavigationMapboxMap theNavigationMap = new NavigationMapboxMap(mapRoute);
    OnRouteSelectionChangeListener listener = mock(OnRouteSelectionChangeListener.class);

    theNavigationMap.setOnRouteSelectionChangeListener(listener);

    verify(mapRoute).setOnRouteSelectionChangeListener(eq(listener));
  }

  @Test
  public void showAlternativeRoutes_correctVisibilityIsSet() {
    NavigationMapRoute mapRoute = mock(NavigationMapRoute.class);
    NavigationMapboxMap theNavigationMap = new NavigationMapboxMap(mapRoute);
    boolean notVisible = false;

    theNavigationMap.showAlternativeRoutes(notVisible);

    verify(mapRoute).showAlternativeRoutes(notVisible);
  }

  @Test
  public void addOnWayNameChangedListener_listenerIsAddedToMapWayname() {
    MapWayName mapWayName = mock(MapWayName.class);
    MapFpsDelegate mapFpsDelegate = mock(MapFpsDelegate.class);
    NavigationMapboxMap theNavigationMap = new NavigationMapboxMap(mapWayName, mapFpsDelegate);
    OnWayNameChangedListener listener = mock(OnWayNameChangedListener.class);

    boolean isAdded = theNavigationMap.addOnWayNameChangedListener(listener);

    assertTrue(isAdded);
  }

  @Test
  public void removeOnWayNameChangedListener_listenerIsRemovedFromMapWayname() {
    MapWayName mapWayName = mock(MapWayName.class);
    MapFpsDelegate mapFpsDelegate = mock(MapFpsDelegate.class);
    NavigationMapboxMap theNavigationMap = new NavigationMapboxMap(mapWayName, mapFpsDelegate);
    OnWayNameChangedListener listener = mock(OnWayNameChangedListener.class);

    theNavigationMap.addOnWayNameChangedListener(listener);
    boolean isRemoved = theNavigationMap.removeOnWayNameChangedListener(listener);

    assertTrue(isRemoved);
  }

  @Test
  public void updateMapFpsThrottle_mapFpsDelegateIsUpdated() {
    MapWayName mapWayName = mock(MapWayName.class);
    MapFpsDelegate mapFpsDelegate = mock(MapFpsDelegate.class);
    NavigationMapboxMap theNavigationMap = new NavigationMapboxMap(mapWayName, mapFpsDelegate);
    int maxFpsThreshold = 10;

    theNavigationMap.updateMapFpsThrottle(maxFpsThreshold);

    verify(mapFpsDelegate).updateMaxFpsThreshold(maxFpsThreshold);
  }

  @Test
  public void updateMapFpsThrottleEnabled_mapFpsDelegateIsEnabled() {
    MapWayName mapWayName = mock(MapWayName.class);
    MapFpsDelegate mapFpsDelegate = mock(MapFpsDelegate.class);
    NavigationMapboxMap theNavigationMap = new NavigationMapboxMap(mapWayName, mapFpsDelegate);
    boolean isEnabled = false;

    theNavigationMap.updateMapFpsThrottleEnabled(isEnabled);

    verify(mapFpsDelegate).updateEnabled(isEnabled);
  }

  @Test
  public void onStart_mapFpsOnStartIsCalled() {
    MapWayName mapWayName = mock(MapWayName.class);
    MapFpsDelegate mapFpsDelegate = mock(MapFpsDelegate.class);
    NavigationMapRoute mapRoute = mock(NavigationMapRoute.class);
    NavigationCamera mapCamera = mock(NavigationCamera.class);
    LocationFpsDelegate locationFpsDelegate = mock(LocationFpsDelegate.class);
    NavigationMapboxMap theNavigationMap = new NavigationMapboxMap(mapWayName, mapFpsDelegate,
      mapRoute, mapCamera, locationFpsDelegate);

    theNavigationMap.onStart();

    verify(mapFpsDelegate).onStart();
  }

  @Test
  public void onStop_mapFpsOnStopIsCalled() {
    MapWayName mapWayName = mock(MapWayName.class);
    MapFpsDelegate mapFpsDelegate = mock(MapFpsDelegate.class);
    NavigationMapRoute mapRoute = mock(NavigationMapRoute.class);
    NavigationCamera mapCamera = mock(NavigationCamera.class);
    LocationFpsDelegate locationFpsDelegate = mock(LocationFpsDelegate.class);
    NavigationMapboxMap theNavigationMap = new NavigationMapboxMap(mapWayName, mapFpsDelegate,
      mapRoute, mapCamera, locationFpsDelegate);

    theNavigationMap.onStop();

    verify(mapFpsDelegate).onStop();
  }

  @Test
  public void onStart_mapWayNameOnStartIsCalled() {
    MapWayName mapWayName = mock(MapWayName.class);
    MapFpsDelegate mapFpsDelegate = mock(MapFpsDelegate.class);
    NavigationMapRoute mapRoute = mock(NavigationMapRoute.class);
    NavigationCamera mapCamera = mock(NavigationCamera.class);
    LocationFpsDelegate locationFpsDelegate = mock(LocationFpsDelegate.class);
    NavigationMapboxMap theNavigationMap = new NavigationMapboxMap(mapWayName, mapFpsDelegate,
      mapRoute, mapCamera, locationFpsDelegate);

    theNavigationMap.onStart();

    verify(mapWayName).onStart();
  }

  @Test
  public void onStop_mapWayNameOnStopIsCalled() {
    MapWayName mapWayName = mock(MapWayName.class);
    MapFpsDelegate mapFpsDelegate = mock(MapFpsDelegate.class);
    NavigationMapRoute mapRoute = mock(NavigationMapRoute.class);
    NavigationCamera mapCamera = mock(NavigationCamera.class);
    LocationFpsDelegate locationFpsDelegate = mock(LocationFpsDelegate.class);
    NavigationMapboxMap theNavigationMap = new NavigationMapboxMap(mapWayName, mapFpsDelegate,
      mapRoute, mapCamera, locationFpsDelegate);

    theNavigationMap.onStop();

    verify(mapWayName).onStop();
  }

  @Test
  public void onInitializeWayName_existingV7StreetSourceIsUsed() {
    Style style = mock(Style.class);
    String urlV7 = "mapbox://mapbox.mapbox-streets-v7";
    List<Source> sources = buildMockSourcesWith(urlV7);
    when(style.getSources()).thenReturn(sources);
    MapboxMap mapboxMap = mock(MapboxMap.class);
    when(mapboxMap.getStyle()).thenReturn(style);
    MapLayerInteractor layerInteractor = mock(MapLayerInteractor.class);
    MapPaddingAdjustor adjustor = mock(MapPaddingAdjustor.class);

    new NavigationMapboxMap(mapboxMap, layerInteractor, adjustor);

    verify(layerInteractor).addStreetsLayer("composite", "road_label");
  }

  @Test
  public void updateLocationFpsThrottleEnabled_locationFpsUpdateEnabledIsSet() {
    MapWayName mapWayName = mock(MapWayName.class);
    MapFpsDelegate mapFpsDelegate = mock(MapFpsDelegate.class);
    NavigationMapRoute mapRoute = mock(NavigationMapRoute.class);
    NavigationCamera mapCamera = mock(NavigationCamera.class);
    LocationFpsDelegate locationFpsDelegate = mock(LocationFpsDelegate.class);
    NavigationMapboxMap theNavigationMap = new NavigationMapboxMap(mapWayName, mapFpsDelegate,
      mapRoute, mapCamera, locationFpsDelegate);

    theNavigationMap.updateLocationFpsThrottleEnabled(false);

    verify(locationFpsDelegate).updateEnabled(eq(false));
  }

  @Test
  public void onStart_locationFpsUpdateOnStartIsCalled() {
    MapWayName mapWayName = mock(MapWayName.class);
    MapFpsDelegate mapFpsDelegate = mock(MapFpsDelegate.class);
    NavigationMapRoute mapRoute = mock(NavigationMapRoute.class);
    NavigationCamera mapCamera = mock(NavigationCamera.class);
    LocationFpsDelegate locationFpsDelegate = mock(LocationFpsDelegate.class);
    NavigationMapboxMap theNavigationMap = new NavigationMapboxMap(mapWayName, mapFpsDelegate,
      mapRoute, mapCamera, locationFpsDelegate);

    theNavigationMap.onStart();

    verify(locationFpsDelegate).onStart();
  }

  @Test
  public void onStop_locationFpsUpdateOnStopIsCalled() {
    MapWayName mapWayName = mock(MapWayName.class);
    MapFpsDelegate mapFpsDelegate = mock(MapFpsDelegate.class);
    NavigationMapRoute mapRoute = mock(NavigationMapRoute.class);
    NavigationCamera mapCamera = mock(NavigationCamera.class);
    LocationFpsDelegate locationFpsDelegate = mock(LocationFpsDelegate.class);
    NavigationMapboxMap theNavigationMap = new NavigationMapboxMap(mapWayName, mapFpsDelegate,
      mapRoute, mapCamera, locationFpsDelegate);

    theNavigationMap.onStop();

    verify(locationFpsDelegate).onStop();
  }

  @Test
  public void onInitializeWayName_exisitingV8StreetSourceIsUsed() {
    Style style = mock(Style.class);
    String urlV7 = "mapbox://mapbox.mapbox-streets-v8";
    List<Source> sources = buildMockSourcesWith(urlV7);
    when(style.getSources()).thenReturn(sources);
    MapboxMap mapboxMap = mock(MapboxMap.class);
    when(mapboxMap.getStyle()).thenReturn(style);
    MapLayerInteractor layerInteractor = mock(MapLayerInteractor.class);
    MapPaddingAdjustor adjustor = mock(MapPaddingAdjustor.class);

    new NavigationMapboxMap(mapboxMap, layerInteractor, adjustor);

    verify(layerInteractor).addStreetsLayer("composite", "road");
  }

  @Test
  public void addDestinationMarker_navigationSymbolManagerReceivesPosition() {
    Point position = mock(Point.class);
    NavigationSymbolManager navigationSymbolManager = mock(NavigationSymbolManager.class);
    NavigationMapboxMap map = new NavigationMapboxMap(navigationSymbolManager);

    map.addDestinationMarker(position);

    verify(navigationSymbolManager).addDestinationMarkerFor(position);
  }

  @Test
  public void addCustomMarker_navigationSymbolManagerReceivesOptions() {
    SymbolOptions options = mock(SymbolOptions.class);
    NavigationSymbolManager navigationSymbolManager = mock(NavigationSymbolManager.class);
    NavigationMapboxMap map = new NavigationMapboxMap(navigationSymbolManager);

    map.addCustomMarker(options);

    verify(navigationSymbolManager).addCustomSymbolFor(options);
  }

  @Test
  public void clearMarkerWithId_navigationSymbolManagerReceivesId() {
    long  markerId = 911L;
    NavigationSymbolManager navigationSymbolManager = mock(NavigationSymbolManager.class);
    NavigationMapboxMap map = new NavigationMapboxMap(navigationSymbolManager);

    map.clearMarkerWithId(markerId);

    verify(navigationSymbolManager).clearSymbolWithId(markerId);
  }

  @Test
  public void clearMarkersWithIconImageProperty_navigationSymbolManagerReceivesIconImage() {
    String markerIconImage = "icon-image";
    NavigationSymbolManager navigationSymbolManager = mock(NavigationSymbolManager.class);
    NavigationMapboxMap map = new NavigationMapboxMap(navigationSymbolManager);

    map.clearMarkersWithIconImageProperty(markerIconImage);

    verify(navigationSymbolManager).clearSymbolsWithIconImageProperty(markerIconImage);
  }

  @Test
  public void clearMarkers_navigationSymbolManagerClearAllMarkerSymbols() {
    NavigationSymbolManager navigationSymbolManager = mock(NavigationSymbolManager.class);
    NavigationMapboxMap map = new NavigationMapboxMap(navigationSymbolManager);

    map.clearMarkers();

    verify(navigationSymbolManager).clearAllMarkerSymbols();
  }

  @Test
  public void setCamera_setsCameraOnMapCamera() {
    MapWayName mapWayName = mock(MapWayName.class);
    MapFpsDelegate mapFpsDelegate = mock(MapFpsDelegate.class);
    NavigationMapRoute mapRoute = mock(NavigationMapRoute.class);
    NavigationCamera mapCamera = mock(NavigationCamera.class);
    LocationFpsDelegate locationFpsDelegate = mock(LocationFpsDelegate.class);
    NavigationMapboxMap theNavigationMap = new NavigationMapboxMap(mapWayName, mapFpsDelegate,
            mapRoute, mapCamera, locationFpsDelegate);
    SimpleCamera simpleCamera = new SimpleCamera();

    theNavigationMap.setCamera(simpleCamera);

    verify(mapCamera).setCamera(simpleCamera);
  }

  @Test
  public void enableVanishingRouteLine() {
    LocationComponent locationComponent = mock(LocationComponent.class);
    MapWayName mapWayName = mock(MapWayName.class);
    MapFpsDelegate mapFpsDelegate = mock(MapFpsDelegate.class);
    NavigationMapRoute mapRoute = mock(NavigationMapRoute.class);
    NavigationCamera mapCamera = mock(NavigationCamera.class);
    LocationFpsDelegate locationFpsDelegate = mock(LocationFpsDelegate.class);
    NavigationMapboxMap theNavigationMap = new NavigationMapboxMap(
            mapWayName,
            mapFpsDelegate,
            mapRoute,
            mapCamera,
            locationFpsDelegate,
            locationComponent,
            false);

    theNavigationMap.enableVanishingRouteLine();

    verify(mapRoute).setVanishRouteLineEnabled(true);
  }

  @Test
  public void disableVanishingRouteLine() {
    LocationComponent locationComponent = mock(LocationComponent.class);
    MapWayName mapWayName = mock(MapWayName.class);
    MapFpsDelegate mapFpsDelegate = mock(MapFpsDelegate.class);
    NavigationMapRoute mapRoute = mock(NavigationMapRoute.class);
    NavigationCamera mapCamera = mock(NavigationCamera.class);
    LocationFpsDelegate locationFpsDelegate = mock(LocationFpsDelegate.class);
    NavigationMapboxMap theNavigationMap = new NavigationMapboxMap(
            mapWayName,
            mapFpsDelegate,
            mapRoute,
            mapCamera,
            locationFpsDelegate,
            locationComponent,
            true);

    theNavigationMap.disableVanishingRouteLine();

    verify(mapRoute).setVanishRouteLineEnabled(false);
  }

  @Test
  public void locationUpdateOnNewProgressUpdate() {
    LocationComponent locationComponent = mock(LocationComponent.class);
    MapWayName mapWayName = mock(MapWayName.class);
    MapFpsDelegate mapFpsDelegate = mock(MapFpsDelegate.class);
    NavigationMapRoute mapRoute = mock(NavigationMapRoute.class);
    NavigationCamera mapCamera = mock(NavigationCamera.class);
    LocationFpsDelegate locationFpsDelegate = mock(LocationFpsDelegate.class);
    MapboxMap mapboxMap = mock(MapboxMap.class);
    when(mapboxMap.getProjection()).thenReturn(mock(Projection.class));
    NavigationMapboxMap theNavigationMap = new NavigationMapboxMap(
        mapboxMap,
        mapWayName,
        mapFpsDelegate,
        mapRoute,
        mapCamera,
        locationFpsDelegate,
        locationComponent,
        false);

    MapboxNavigation mapboxNavigation = mock(MapboxNavigation.class);
    Location targetLocation = mock(Location.class);
    List<Location> keyPoints = Collections.emptyList();
    MapMatcherResult mapMatcherResult = mock(MapMatcherResult.class);
    when(mapMatcherResult.getEnhancedLocation()).thenReturn(targetLocation);
    when(mapMatcherResult.getKeyPoints()).thenReturn(keyPoints);
    when(mapMatcherResult.isTeleport()).thenReturn(false);
    ArgumentCaptor<MapMatcherResultObserver> observerArgumentCaptor =
        ArgumentCaptor.forClass(MapMatcherResultObserver.class);
    theNavigationMap.addProgressChangeListener(mapboxNavigation);
    verify(mapboxNavigation).registerMapMatcherResultObserver(observerArgumentCaptor.capture());

    observerArgumentCaptor.getValue().onNewMapMatcherResult(mapMatcherResult);

    LocationUpdate expected = new LocationUpdate.Builder()
        .location(targetLocation)
        .intermediatePoints(keyPoints)
        .build();
    verify(locationComponent).forceLocationUpdate(eq(expected));
  }

  @Test
  public void locationUpdateOnNewProgressUpdate_teleport() {
    LocationComponent locationComponent = mock(LocationComponent.class);
    MapWayName mapWayName = mock(MapWayName.class);
    MapFpsDelegate mapFpsDelegate = mock(MapFpsDelegate.class);
    NavigationMapRoute mapRoute = mock(NavigationMapRoute.class);
    NavigationCamera mapCamera = mock(NavigationCamera.class);
    LocationFpsDelegate locationFpsDelegate = mock(LocationFpsDelegate.class);
    MapboxMap mapboxMap = mock(MapboxMap.class);
    when(mapboxMap.getProjection()).thenReturn(mock(Projection.class));
    NavigationMapboxMap theNavigationMap = new NavigationMapboxMap(
        mapboxMap,
        mapWayName,
        mapFpsDelegate,
        mapRoute,
        mapCamera,
        locationFpsDelegate,
        locationComponent,
        false);

    MapboxNavigation mapboxNavigation = mock(MapboxNavigation.class);
    Location targetLocation = mock(Location.class);
    List<Location> keyPoints = Collections.emptyList();
    MapMatcherResult mapMatcherResult = mock(MapMatcherResult.class);
    when(mapMatcherResult.getEnhancedLocation()).thenReturn(targetLocation);
    when(mapMatcherResult.getKeyPoints()).thenReturn(keyPoints);
    when(mapMatcherResult.isTeleport()).thenReturn(true);
    ArgumentCaptor<MapMatcherResultObserver> observerArgumentCaptor =
        ArgumentCaptor.forClass(MapMatcherResultObserver.class);
    theNavigationMap.addProgressChangeListener(mapboxNavigation);
    verify(mapboxNavigation).registerMapMatcherResultObserver(observerArgumentCaptor.capture());

    observerArgumentCaptor.getValue().onNewMapMatcherResult(mapMatcherResult);

    LocationUpdate expected = new LocationUpdate.Builder()
        .location(targetLocation)
        .intermediatePoints(keyPoints)
        .animationDuration(0L)
        .build();
    verify(locationComponent).forceLocationUpdate(eq(expected));
  }

  @Test
  public void locationUpdateOnNewProgressUpdate_keyPoints() {
    LocationComponent locationComponent = mock(LocationComponent.class);
    MapWayName mapWayName = mock(MapWayName.class);
    MapFpsDelegate mapFpsDelegate = mock(MapFpsDelegate.class);
    NavigationMapRoute mapRoute = mock(NavigationMapRoute.class);
    NavigationCamera mapCamera = mock(NavigationCamera.class);
    LocationFpsDelegate locationFpsDelegate = mock(LocationFpsDelegate.class);
    MapboxMap mapboxMap = mock(MapboxMap.class);
    when(mapboxMap.getProjection()).thenReturn(mock(Projection.class));
    NavigationMapboxMap theNavigationMap = new NavigationMapboxMap(
        mapboxMap,
        mapWayName,
        mapFpsDelegate,
        mapRoute,
        mapCamera,
        locationFpsDelegate,
        locationComponent,
        false);

    MapboxNavigation mapboxNavigation = mock(MapboxNavigation.class);
    Location targetLocation = mock(Location.class);
    Location intermediateLocation = mock(Location.class);
    List<Location> keyPoints = new ArrayList<>();
    keyPoints.add(intermediateLocation);
    keyPoints.add(targetLocation);
    MapMatcherResult mapMatcherResult = mock(MapMatcherResult.class);
    when(mapMatcherResult.getEnhancedLocation()).thenReturn(targetLocation);
    when(mapMatcherResult.getKeyPoints()).thenReturn(keyPoints);
    when(mapMatcherResult.isTeleport()).thenReturn(false);
    ArgumentCaptor<MapMatcherResultObserver> observerArgumentCaptor =
        ArgumentCaptor.forClass(MapMatcherResultObserver.class);
    theNavigationMap.addProgressChangeListener(mapboxNavigation);
    verify(mapboxNavigation).registerMapMatcherResultObserver(observerArgumentCaptor.capture());

    observerArgumentCaptor.getValue().onNewMapMatcherResult(mapMatcherResult);

    List<Location> expectedIntermediateLocations = new ArrayList<>();
    expectedIntermediateLocations.add(intermediateLocation);
    LocationUpdate expected = new LocationUpdate.Builder()
        .location(targetLocation)
        .intermediatePoints(expectedIntermediateLocations)
        .build();
    verify(locationComponent).forceLocationUpdate(eq(expected));
  }

  private List<Source> buildMockSourcesWith(String url) {
    List<Source> sources = new ArrayList<>();
    VectorSource vectorSource1 = mock(VectorSource.class);
    VectorSource vectorSource2 = mock(VectorSource.class);
    when(vectorSource2.getId()).thenReturn("composite");
    when(vectorSource2.getUrl()).thenReturn(url);
    GeoJsonSource geoJsonSource = mock(GeoJsonSource.class);
    sources.add(vectorSource1);
    sources.add(vectorSource2);
    sources.add(geoJsonSource);
    return sources;
  }
}