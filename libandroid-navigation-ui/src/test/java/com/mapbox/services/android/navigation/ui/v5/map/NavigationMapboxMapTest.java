package com.mapbox.services.android.navigation.ui.v5.map;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.ui.v5.route.OnRouteSelectionChangeListener;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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
    NavigationMapboxMap theNavigationMap = new NavigationMapboxMap(mapWayName);
    OnWayNameChangedListener listener = mock(OnWayNameChangedListener.class);

    theNavigationMap.addOnWayNameChangedListener(listener);

    verify(mapWayName).addOnWayNameChangedListener(listener);
  }

  @Test
  public void removeOnWayNameChangedListener_listenerIsRemovedFromMapWayname() {
    MapWayName mapWayName = mock(MapWayName.class);
    NavigationMapboxMap theNavigationMap = new NavigationMapboxMap(mapWayName);
    OnWayNameChangedListener listener = mock(OnWayNameChangedListener.class);

    theNavigationMap.removeOnWayNameChangedListener(listener);

    verify(mapWayName).removeOnWayNameChangedListener(listener);
  }

  @Test
  public void updateMapFpsThrottle_mapFpsDelegateIsUpdated() {
    MapFpsDelegate delegate = mock(MapFpsDelegate.class);
    NavigationMapboxMap theNavigationMap = new NavigationMapboxMap(delegate);
    int maxFpsThreshold = 10;

    theNavigationMap.updateMapFpsThrottle(maxFpsThreshold);

    verify(delegate).updateMaxFpsThreshold(maxFpsThreshold);
  }

  @Test
  public void updateMapFpsThrottleEnabled_mapFpsDelegateIsEnabled() {
    MapFpsDelegate delegate = mock(MapFpsDelegate.class);
    NavigationMapboxMap theNavigationMap = new NavigationMapboxMap(delegate);
    boolean isEnabled = false;

    theNavigationMap.updateMapFpsThrottleEnabled(isEnabled);

    verify(delegate).updateEnabled(isEnabled);
  }
}