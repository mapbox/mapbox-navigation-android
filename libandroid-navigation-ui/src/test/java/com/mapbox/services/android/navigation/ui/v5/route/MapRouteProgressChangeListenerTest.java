package com.mapbox.services.android.navigation.ui.v5.route;

import android.location.Location;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import org.junit.Test;

import edu.emory.mathcs.backport.java.util.Collections;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MapRouteProgressChangeListenerTest {

  @Test
  public void onProgressChange_newRouteWithEmptyDirectionsRouteList() {
    NavigationMapRoute mapRoute = mock(NavigationMapRoute.class);
    when(mapRoute.retrieveDirectionsRoutes()).thenReturn(Collections.emptyList());
    when(mapRoute.retrievePrimaryRouteIndex()).thenReturn(0);
    MapRouteProgressChangeListener progressChangeListener = new MapRouteProgressChangeListener(mapRoute);
    RouteProgress routeProgress = mock(RouteProgress.class);
    DirectionsRoute newRoute = mock(DirectionsRoute.class);
    when(routeProgress.directionsRoute()).thenReturn(newRoute);

    progressChangeListener.onProgressChange(mock(Location.class), routeProgress);

    verify(mapRoute).addRoute(eq(newRoute));
  }
}