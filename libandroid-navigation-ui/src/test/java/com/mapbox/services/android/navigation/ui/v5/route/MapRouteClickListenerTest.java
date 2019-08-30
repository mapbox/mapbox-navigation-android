package com.mapbox.services.android.navigation.ui.v5.route;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.core.constants.Constants;
import com.mapbox.geojson.LineString;
import com.mapbox.mapboxsdk.geometry.LatLng;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MapRouteClickListenerTest {

  @Test
  public void checksOnRouteSelectionChangeListenerIsCalledWhenClickedRouteIsFound() {
    DirectionsRoute anyRoute = buildMockDirectionsRoute();
    List<DirectionsRoute> anyDirectionsRoutes = buildDirectionsRoutes(anyRoute);
    LineString anyRouteGeometry = LineString.fromPolyline(anyRoute.geometry(), Constants.PRECISION_6);
    HashMap<LineString, DirectionsRoute> anyLineStringDirectionsRouteMap =
      buildLineStringDirectionsRouteHashMap(anyRoute, anyRouteGeometry);
    MapRouteLine mockedMapRouteLine = buildMockMapRouteLine(true, anyLineStringDirectionsRouteMap);
    when(mockedMapRouteLine.updatePrimaryRouteIndex(anyInt())).thenReturn(true);
    when(mockedMapRouteLine.retrieveDirectionsRoutes()).thenReturn(anyDirectionsRoutes);
    MapRouteClickListener theMapRouteClickListener = new MapRouteClickListener(mockedMapRouteLine);
    OnRouteSelectionChangeListener mockedOnRouteSelectionChangeListener =
      buildMockOnRouteSelectionChangeListener(theMapRouteClickListener);
    LatLng mockedPoint = mock(LatLng.class);

    theMapRouteClickListener.onMapClick(mockedPoint);

    verify(mockedOnRouteSelectionChangeListener).onNewPrimaryRouteSelected(any(DirectionsRoute.class));
  }

  @Test
  public void checksOnRouteSelectionChangeListenerIsNotCalledWhenRouteIsNotVisible() {
    HashMap<LineString, DirectionsRoute> mockedLineStringDirectionsRouteMap = mock(HashMap.class);
    MapRouteLine mockedMapRouteLine = buildMockMapRouteLine(false, mockedLineStringDirectionsRouteMap);
    MapRouteClickListener theMapRouteClickListener = new MapRouteClickListener(mockedMapRouteLine);
    OnRouteSelectionChangeListener mockedOnRouteSelectionChangeListener =
      buildMockOnRouteSelectionChangeListener(theMapRouteClickListener);
    LatLng mockedPoint = mock(LatLng.class);

    theMapRouteClickListener.onMapClick(mockedPoint);

    verify(mockedOnRouteSelectionChangeListener, never()).onNewPrimaryRouteSelected(any(DirectionsRoute.class));
  }

  private DirectionsRoute buildMockDirectionsRoute() {
    DirectionsRoute anyRoute = mock(DirectionsRoute.class);
    when(anyRoute.geometry()).thenReturn("awbagAzavnhFp`@~fGr~Ya|BhcBwcYbr\\u{C`tZ~{H~vrBsge@bdo@`kc@dqpAckUbmn" +
      "@sphAjnDovu@zviDgasDpa^ixsBbmy@{ubBvou@ajy@|}\\y~q@dycAcotGj{v@cdr@lyUwpC");
    when(anyRoute.routeIndex()).thenReturn("1");
    return anyRoute;
  }

  private List<DirectionsRoute> buildDirectionsRoutes(DirectionsRoute anyRoute) {
    List<DirectionsRoute> anyDirectionsRoutes = new ArrayList<>();
    anyDirectionsRoutes.add(anyRoute);
    return anyDirectionsRoutes;
  }

  private HashMap<LineString, DirectionsRoute> buildLineStringDirectionsRouteHashMap(DirectionsRoute anyRoute,
                                                                                     LineString anyRouteGeometry) {
    HashMap<LineString, DirectionsRoute> anyLineStringDirectionsRouteMap = new HashMap<>();
    anyLineStringDirectionsRouteMap.put(anyRouteGeometry, anyRoute);
    return anyLineStringDirectionsRouteMap;
  }

  private MapRouteLine buildMockMapRouteLine(boolean isVisible,
                                             HashMap<LineString, DirectionsRoute> lineStringDirectionsRouteMap) {
    MapRouteLine mockedMapRouteLine = mock(MapRouteLine.class);
    when(mockedMapRouteLine.retrieveVisibility()).thenReturn(isVisible);
    when(mockedMapRouteLine.retrieveRouteLineStrings()).thenReturn(lineStringDirectionsRouteMap);
    return mockedMapRouteLine;
  }

  private OnRouteSelectionChangeListener buildMockOnRouteSelectionChangeListener(MapRouteClickListener theMapRouteClickListener) {
    OnRouteSelectionChangeListener mockedOnRouteSelectionChangeListener = mock(OnRouteSelectionChangeListener.class);
    theMapRouteClickListener.setOnRouteSelectionChangeListener(mockedOnRouteSelectionChangeListener);
    return mockedOnRouteSelectionChangeListener;
  }
}