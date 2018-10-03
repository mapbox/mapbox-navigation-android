package com.mapbox.services.android.navigation.ui.v5.route;

import android.location.Location;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import org.junit.Test;

import edu.emory.mathcs.backport.java.util.Collections;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class MapRouteProgressChangeListenerTest {

  @Test
  public void onProgressChange_newRouteWithEmptyDirectionsRouteList() {
    MapRouteLine routeLine = mock(MapRouteLine.class);
    when(routeLine.retrieveDirectionsRoutes()).thenReturn(Collections.emptyList());
    when(routeLine.retrievePrimaryRouteIndex()).thenReturn(0);
    MapRouteArrow routeArrow = mock(MapRouteArrow.class);
    MapRouteProgressChangeListener progressChangeListener = new MapRouteProgressChangeListener(routeLine, routeArrow);
    RouteProgress routeProgress = mock(RouteProgress.class);
    DirectionsRoute newRoute = mock(DirectionsRoute.class);
    when(routeProgress.directionsRoute()).thenReturn(newRoute);

    progressChangeListener.onProgressChange(mock(Location.class), routeProgress);

    verify(routeLine).draw(eq(newRoute));
  }

  @Test
  public void onProgressChange_isVisibleFalseIgnoresProgress() {
    MapRouteLine routeLine = mock(MapRouteLine.class);
    MapRouteArrow routeArrow = mock(MapRouteArrow.class);
    MapRouteProgressChangeListener progressChangeListener = new MapRouteProgressChangeListener(routeLine, routeArrow);
    progressChangeListener.updateVisibility(false);

    progressChangeListener.onProgressChange(mock(Location.class), mock(RouteProgress.class));

    verifyZeroInteractions(routeLine);
  }

  @Test
  public void onProgressChange_isVisibleTrueProcessesProgress() {
    MapRouteLine routeLine = mock(MapRouteLine.class);
    MapRouteArrow routeArrow = mock(MapRouteArrow.class);
    MapRouteProgressChangeListener progressChangeListener = new MapRouteProgressChangeListener(routeLine, routeArrow);
    progressChangeListener.updateVisibility(true);

    progressChangeListener.onProgressChange(mock(Location.class), mock(RouteProgress.class));

    verify(routeLine).retrieveDirectionsRoutes();
  }
}