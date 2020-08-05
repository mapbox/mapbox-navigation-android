package com.mapbox.navigation.ui.route;

import androidx.lifecycle.LifecycleOwner;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.navigation.base.trip.model.RouteProgress;
import com.mapbox.navigation.core.MapboxNavigation;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import edu.emory.mathcs.backport.java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class NavigationMapRouteTest {

  @Test
  public void checksMapClickListenerIsAddedAtConstructionTime() {
    MapboxNavigation mockedNavigation = mock(MapboxNavigation.class);
    MapView mockedMapView = mock(MapView.class);
    MapboxMap mockedMapboxMap = mock(MapboxMap.class);
    int mockedStyleRes = 0;
    MapRouteClickListener mockedMapClickListener = mock(MapRouteClickListener.class);
    MapView.OnDidFinishLoadingStyleListener mockedDidFinishLoadingStyleListener =
      mock(MapView.OnDidFinishLoadingStyleListener.class);
    MapRouteProgressChangeListener mockedProgressChangeListener = mock(MapRouteProgressChangeListener.class);
    LifecycleOwner mockedLifecycleOwner = mock(LifecycleOwner.class);

    new NavigationMapRoute(mockedNavigation, mockedMapView, mockedMapboxMap, mockedLifecycleOwner, mockedStyleRes, "",
      mockedMapClickListener, mockedDidFinishLoadingStyleListener, mockedProgressChangeListener);

    verify(mockedMapboxMap, times(1)).addOnMapClickListener(eq(mockedMapClickListener));
  }

  @Test
  public void checksDidFinishLoadingStyleListenerIsAddedAtConstructionTime() {
    MapboxNavigation mockedNavigation = mock(MapboxNavigation.class);
    MapView mockedMapView = mock(MapView.class);
    MapboxMap mockedMapboxMap = mock(MapboxMap.class);
    int mockedStyleRes = 0;
    MapRouteClickListener mockedMapClickListener = mock(MapRouteClickListener.class);
    MapView.OnDidFinishLoadingStyleListener mockedDidFinishLoadingStyleListener =
      mock(MapView.OnDidFinishLoadingStyleListener.class);
    MapRouteProgressChangeListener mockedProgressChangeListener = mock(MapRouteProgressChangeListener.class);
    LifecycleOwner mockedLifecycleOwner = mock(LifecycleOwner.class);

    new NavigationMapRoute(mockedNavigation, mockedMapView, mockedMapboxMap, mockedLifecycleOwner, mockedStyleRes, "",
      mockedMapClickListener, mockedDidFinishLoadingStyleListener, mockedProgressChangeListener);

    verify(mockedMapView, times(1))
      .addOnDidFinishLoadingStyleListener(eq(mockedDidFinishLoadingStyleListener));
  }

  @Test
  public void checksMapRouteProgressChangeListenerIsAddedAtConstructionTime() {
    MapboxNavigation mockedNavigation = mock(MapboxNavigation.class);
    MapView mockedMapView = mock(MapView.class);
    MapboxMap mockedMapboxMap = mock(MapboxMap.class);
    int mockedStyleRes = 0;
    MapRouteClickListener mockedMapClickListener = mock(MapRouteClickListener.class);
    MapView.OnDidFinishLoadingStyleListener mockedDidFinishLoadingStyleListener =
      mock(MapView.OnDidFinishLoadingStyleListener.class);
    MapRouteProgressChangeListener mockedProgressChangeListener = mock(MapRouteProgressChangeListener.class);
    LifecycleOwner mockedLifecycleOwner = mock(LifecycleOwner.class);
    new NavigationMapRoute(mockedNavigation, mockedMapView, mockedMapboxMap, mockedLifecycleOwner,
      mockedStyleRes, "", mockedMapClickListener, mockedDidFinishLoadingStyleListener,
      mockedProgressChangeListener);

    verify(mockedNavigation, times(1))
      .registerRouteProgressObserver(eq(mockedProgressChangeListener));
  }

  @Test
  public void checksMapClickListenerIsNotAddedIfIsMapClickListenerAdded() {
    MapboxNavigation mockedNavigation = mock(MapboxNavigation.class);
    MapView mockedMapView = mock(MapView.class);
    MapboxMap mockedMapboxMap = mock(MapboxMap.class);
    int mockedStyleRes = 0;
    MapRouteClickListener mockedMapClickListener = mock(MapRouteClickListener.class);
    MapView.OnDidFinishLoadingStyleListener mockedDidFinishLoadingStyleListener =
      mock(MapView.OnDidFinishLoadingStyleListener.class);
    MapRouteProgressChangeListener mockedProgressChangeListener = mock(MapRouteProgressChangeListener.class);
    LifecycleOwner mockedLifecycleOwner = mock(LifecycleOwner.class);
    MapRouteLine mockedMapRouteLine = mock(MapRouteLine.class);
    MapRouteArrow mockedMapRouteArrow = mock(MapRouteArrow.class);
    NavigationMapRoute theNavigationMapRoute = new NavigationMapRoute(mockedNavigation, mockedMapView, mockedMapboxMap,
            mockedLifecycleOwner, mockedStyleRes, "", mockedMapClickListener, mockedDidFinishLoadingStyleListener,
            mockedProgressChangeListener, mockedMapRouteLine, mockedMapRouteArrow);

    theNavigationMapRoute.onStart();

    verify(mockedMapboxMap, times(1)).addOnMapClickListener(eq(mockedMapClickListener));
  }

  @Test
  public void checksDidFinishLoadingStyleListenerIsNotAddedIfIsDidFinishLoadingStyleListenerAdded() {
    MapboxNavigation mockedNavigation = mock(MapboxNavigation.class);
    MapView mockedMapView = mock(MapView.class);
    MapboxMap mockedMapboxMap = mock(MapboxMap.class);
    int mockedStyleRes = 0;
    MapRouteClickListener mockedMapClickListener = mock(MapRouteClickListener.class);
    MapView.OnDidFinishLoadingStyleListener mockedDidFinishLoadingStyleListener =
      mock(MapView.OnDidFinishLoadingStyleListener.class);
    MapRouteProgressChangeListener mockedProgressChangeListener = mock(MapRouteProgressChangeListener.class);
    LifecycleOwner mockedLifecycleOwner = mock(LifecycleOwner.class);
    MapRouteLine mockedMapRouteLine = mock(MapRouteLine.class);
    MapRouteArrow mockedMapRouteArrow = mock(MapRouteArrow.class);
    NavigationMapRoute theNavigationMapRoute = new NavigationMapRoute(mockedNavigation, mockedMapView, mockedMapboxMap,
            mockedLifecycleOwner, mockedStyleRes, "", mockedMapClickListener, mockedDidFinishLoadingStyleListener,
            mockedProgressChangeListener, mockedMapRouteLine, mockedMapRouteArrow);

    theNavigationMapRoute.onStart();

    verify(mockedMapView, times(1))
      .addOnDidFinishLoadingStyleListener(eq(mockedDidFinishLoadingStyleListener));
  }

  @Test
  public void checksMapClickListenerIsRemovedInOnStop() {
    LocationComponent mockLocationComponent = mock(LocationComponent.class);
    MapboxNavigation mockedNavigation = mock(MapboxNavigation.class);
    MapView mockedMapView = mock(MapView.class);
    MapboxMap mockedMapboxMap = mock(MapboxMap.class);
    int mockedStyleRes = 0;
    MapRouteClickListener mockedMapClickListener = mock(MapRouteClickListener.class);
    MapView.OnDidFinishLoadingStyleListener mockedDidFinishLoadingStyleListener =
      mock(MapView.OnDidFinishLoadingStyleListener.class);
    LifecycleOwner mockedLifecycleOwner = mock(LifecycleOwner.class);
    MapRouteProgressChangeListener mockedProgressChangeListener = mock(MapRouteProgressChangeListener.class);
    NavigationMapRoute theNavigationMapRoute = new NavigationMapRoute(mockedNavigation, mockedMapView,
      mockedMapboxMap, mockedLifecycleOwner, mockedStyleRes, "", mockedMapClickListener, mockedDidFinishLoadingStyleListener,
      mockedProgressChangeListener);
    when (mockedMapboxMap.getLocationComponent()).thenReturn(mockLocationComponent);

    theNavigationMapRoute.onStop();

    verify(mockedMapboxMap, times(1)).removeOnMapClickListener(eq(mockedMapClickListener));
  }

  @Test
  public void checksDidFinishLoadingStyleListenerIsRemovedInOnStop() {
    LocationComponent mockLocationComponent = mock(LocationComponent.class);
    MapboxNavigation mockedNavigation = mock(MapboxNavigation.class);
    MapView mockedMapView = mock(MapView.class);
    MapboxMap mockedMapboxMap = mock(MapboxMap.class);
    int mockedStyleRes = 0;
    MapRouteClickListener mockedMapClickListener = mock(MapRouteClickListener.class);
    MapView.OnDidFinishLoadingStyleListener mockedDidFinishLoadingStyleListener =
      mock(MapView.OnDidFinishLoadingStyleListener.class);
    LifecycleOwner mockedLifecycleOwner = mock(LifecycleOwner.class);
    MapRouteProgressChangeListener mockedProgressChangeListener = mock(MapRouteProgressChangeListener.class);
    NavigationMapRoute theNavigationMapRoute = new NavigationMapRoute(mockedNavigation, mockedMapView,
      mockedMapboxMap, mockedLifecycleOwner, mockedStyleRes, "", mockedMapClickListener, mockedDidFinishLoadingStyleListener,
      mockedProgressChangeListener);
    when (mockedMapboxMap.getLocationComponent()).thenReturn(mockLocationComponent);

    theNavigationMapRoute.onStop();

    verify(mockedMapView, times(1))
      .removeOnDidFinishLoadingStyleListener(eq(mockedDidFinishLoadingStyleListener));
  }

  @Test
  public void checksMapRouteProgressChangeListenerIsRemovedInOnStop() {
    LocationComponent mockLocationComponent = mock(LocationComponent.class);
    MapboxNavigation mockedNavigation = mock(MapboxNavigation.class);
    MapView mockedMapView = mock(MapView.class);
    MapboxMap mockedMapboxMap = mock(MapboxMap.class);
    int mockedStyleRes = 0;
    MapRouteClickListener mockedMapClickListener = mock(MapRouteClickListener.class);
    MapView.OnDidFinishLoadingStyleListener mockedDidFinishLoadingStyleListener =
      mock(MapView.OnDidFinishLoadingStyleListener.class);
    LifecycleOwner mockedLifecycleOwner = mock(LifecycleOwner.class);
    MapRouteProgressChangeListener mockedProgressChangeListener = mock(MapRouteProgressChangeListener.class);
    NavigationMapRoute theNavigationMapRoute = new NavigationMapRoute(mockedNavigation, mockedMapView,
      mockedMapboxMap, mockedLifecycleOwner, mockedStyleRes, "", mockedMapClickListener,
      mockedDidFinishLoadingStyleListener, mockedProgressChangeListener);
    when (mockedMapboxMap.getLocationComponent()).thenReturn(mockLocationComponent);

    theNavigationMapRoute.onStop();

    verify(mockedNavigation, times(1))
      .unregisterRouteProgressObserver(eq(mockedProgressChangeListener));
  }

  @Test
  public void removeProgressChangeListener_mapRouteProgressChangeListenerIsRemoved() {
    MapboxNavigation mockedNavigation = mock(MapboxNavigation.class);
    MapView mockedMapView = mock(MapView.class);
    MapboxMap mockedMapboxMap = mock(MapboxMap.class);
    int mockedStyleRes = 0;
    MapRouteClickListener mockedMapClickListener = mock(MapRouteClickListener.class);
    MapView.OnDidFinishLoadingStyleListener mockedDidFinishLoadingStyleListener =
      mock(MapView.OnDidFinishLoadingStyleListener.class);
    MapRouteProgressChangeListener mockedProgressChangeListener = mock(MapRouteProgressChangeListener.class);
    MapRouteLine mockedMapRouteLine = mock(MapRouteLine.class);
    MapRouteArrow mockedMapRouteArrow = mock(MapRouteArrow.class);
    LifecycleOwner mockedLifecycleOwner = mock(LifecycleOwner.class);
    NavigationMapRoute theNavigationMapRoute = new NavigationMapRoute(mockedNavigation, mockedMapView, mockedMapboxMap,
            mockedLifecycleOwner, mockedStyleRes, "", mockedMapClickListener, mockedDidFinishLoadingStyleListener,
      mockedProgressChangeListener, mockedMapRouteLine, mockedMapRouteArrow);

    theNavigationMapRoute.removeProgressChangeListener(mockedNavigation);

    verify(mockedNavigation, times(1))
      .unregisterRouteProgressObserver(eq(mockedProgressChangeListener));
  }

  @Test
  public void addRoutes_whenInputEmptyList() {
    MapboxNavigation mockedNavigation = mock(MapboxNavigation.class);
    MapView mockedMapView = mock(MapView.class);
    MapboxMap mockedMapboxMap = mock(MapboxMap.class);
    int mockedStyleRes = 0;
    MapRouteClickListener mockedMapClickListener = mock(MapRouteClickListener.class);
    MapView.OnDidFinishLoadingStyleListener mockedDidFinishLoadingStyleListener =
      mock(MapView.OnDidFinishLoadingStyleListener.class);
    MapRouteProgressChangeListener mockedProgressChangeListener = mock(MapRouteProgressChangeListener.class);
    MapRouteLine mockedMapRouteLine = mock(MapRouteLine.class);
    MapRouteArrow mockedMapRouteArrow = mock(MapRouteArrow.class);
    List<DirectionsRoute> routes = Collections.emptyList();
    LifecycleOwner mockedLifecycleOwner = mock(LifecycleOwner.class);
    NavigationMapRoute theNavigationMapRoute = new NavigationMapRoute(
      mockedNavigation,
      mockedMapView,
      mockedMapboxMap,
      mockedLifecycleOwner,
      mockedStyleRes,
      "",
      mockedMapClickListener,
      mockedDidFinishLoadingStyleListener,
      mockedProgressChangeListener,
      mockedMapRouteLine,
      mockedMapRouteArrow
    );

    theNavigationMapRoute.addRoutes(routes);

    verify(mockedMapRouteLine).clearRouteData();
  }

  @Test
  public void addRoutes_updatesPrimaryRouteIndex() {
    DirectionsRoute mockRoute = mock(DirectionsRoute.class);
    MapboxNavigation mockedNavigation = mock(MapboxNavigation.class);
    MapView mockedMapView = mock(MapView.class);
    MapboxMap mockedMapboxMap = mock(MapboxMap.class);
    int mockedStyleRes = 0;
    MapRouteClickListener mockedMapClickListener = mock(MapRouteClickListener.class);
    MapView.OnDidFinishLoadingStyleListener mockedDidFinishLoadingStyleListener =
            mock(MapView.OnDidFinishLoadingStyleListener.class);
    MapRouteProgressChangeListener mockedProgressChangeListener = mock(MapRouteProgressChangeListener.class);
    MapRouteLine mockedMapRouteLine = mock(MapRouteLine.class);
    MapRouteArrow mockedMapRouteArrow = mock(MapRouteArrow.class);
    LifecycleOwner mockedLifecycleOwner = mock(LifecycleOwner.class);
    List<DirectionsRoute> routes = Collections.singletonList(mockRoute);
    NavigationMapRoute theNavigationMapRoute = new NavigationMapRoute(mockedNavigation, mockedMapView, mockedMapboxMap,
            mockedLifecycleOwner, mockedStyleRes, "", mockedMapClickListener, mockedDidFinishLoadingStyleListener,
            mockedProgressChangeListener, mockedMapRouteLine, mockedMapRouteArrow);
    when(mockedMapRouteLine.retrieveDirectionsRoutes()).thenReturn(Collections.singletonList(mockRoute));

    theNavigationMapRoute.addRoutes(routes);

    verify(mockedMapRouteLine).updatePrimaryRouteIndex(routes.get(0));
  }

  @Test
  public void addRoutes_drawWhenNewRoutes() {
    DirectionsRoute mockRoute = mock(DirectionsRoute.class);
    MapboxNavigation mockedNavigation = mock(MapboxNavigation.class);
    MapView mockedMapView = mock(MapView.class);
    MapboxMap mockedMapboxMap = mock(MapboxMap.class);
    int mockedStyleRes = 0;
    MapRouteClickListener mockedMapClickListener = mock(MapRouteClickListener.class);
    MapView.OnDidFinishLoadingStyleListener mockedDidFinishLoadingStyleListener =
            mock(MapView.OnDidFinishLoadingStyleListener.class);
    MapRouteProgressChangeListener mockedProgressChangeListener = mock(MapRouteProgressChangeListener.class);
    MapRouteLine mockedMapRouteLine = mock(MapRouteLine.class);
    MapRouteArrow mockedMapRouteArrow = mock(MapRouteArrow.class);
    LifecycleOwner mockedLifecycleOwner = mock(LifecycleOwner.class);
    List<DirectionsRoute> routes = Collections.singletonList(mockRoute);
    NavigationMapRoute theNavigationMapRoute = new NavigationMapRoute(mockedNavigation, mockedMapView, mockedMapboxMap,
            mockedLifecycleOwner, mockedStyleRes, "", mockedMapClickListener, mockedDidFinishLoadingStyleListener,
            mockedProgressChangeListener, mockedMapRouteLine, mockedMapRouteArrow);
    when(mockedMapRouteLine.retrieveDirectionsRoutes()).thenReturn(Collections.singletonList(mock(DirectionsRoute.class)));

    theNavigationMapRoute.addRoutes(routes);

    verify(mockedMapRouteLine).draw(routes);
  }

  @Test
  public void updateRouteVisibilityTo_routeLineVisibilityIsUpdated() {
    MapboxNavigation mockedNavigation = mock(MapboxNavigation.class);
    MapView mockedMapView = mock(MapView.class);
    MapboxMap mockedMapboxMap = mock(MapboxMap.class);
    int mockedStyleRes = 0;
    MapRouteClickListener mockedMapClickListener = mock(MapRouteClickListener.class);
    MapView.OnDidFinishLoadingStyleListener mockedDidFinishLoadingStyleListener =
      mock(MapView.OnDidFinishLoadingStyleListener.class);
    MapRouteProgressChangeListener mockedProgressChangeListener = mock(MapRouteProgressChangeListener.class);
    MapRouteLine mockedMapRouteLine = mock(MapRouteLine.class);
    MapRouteArrow mockedMapRouteArrow = mock(MapRouteArrow.class);
    LifecycleOwner mockedLifecycleOwner = mock(LifecycleOwner.class);
    NavigationMapRoute theNavigationMapRoute = new NavigationMapRoute(mockedNavigation, mockedMapView, mockedMapboxMap,
            mockedLifecycleOwner, mockedStyleRes, "", mockedMapClickListener, mockedDidFinishLoadingStyleListener,
            mockedProgressChangeListener, mockedMapRouteLine, mockedMapRouteArrow);
    boolean isVisible = false;

    theNavigationMapRoute.updateRouteVisibilityTo(isVisible);

    verify(mockedMapRouteLine).updateVisibilityTo(isVisible);
  }

  @Test
  public void addIdentifiableRoutes_whenInputEmptyList() {
    MapboxNavigation mockedNavigation = mock(MapboxNavigation.class);
    MapView mockedMapView = mock(MapView.class);
    MapboxMap mockedMapboxMap = mock(MapboxMap.class);
    int mockedStyleRes = 0;
    MapRouteClickListener mockedMapClickListener = mock(MapRouteClickListener.class);
    MapView.OnDidFinishLoadingStyleListener mockedDidFinishLoadingStyleListener =
            mock(MapView.OnDidFinishLoadingStyleListener.class);
    MapRouteProgressChangeListener mockedProgressChangeListener = mock(MapRouteProgressChangeListener.class);
    MapRouteLine mockedMapRouteLine = mock(MapRouteLine.class);
    MapRouteArrow mockedMapRouteArrow = mock(MapRouteArrow.class);
    List<IdentifiableRoute> routes = Collections.emptyList();
    LifecycleOwner mockedLifecycleOwner = mock(LifecycleOwner.class);
    NavigationMapRoute theNavigationMapRoute = new NavigationMapRoute(mockedNavigation, mockedMapView, mockedMapboxMap,
            mockedLifecycleOwner, mockedStyleRes, "", mockedMapClickListener, mockedDidFinishLoadingStyleListener,
            mockedProgressChangeListener, mockedMapRouteLine, mockedMapRouteArrow);


    theNavigationMapRoute.addIdentifiableRoutes(routes);

    verify(mockedMapRouteLine).drawIdentifiableRoutes(eq(routes));
  }

  @Test
  public void addIdentifiableRoutes_whenNewRoutes() {
    DirectionsRoute mockRoute = mock(DirectionsRoute.class);
    DirectionsRoute anotherMockRoute = mock(DirectionsRoute.class);
    IdentifiableRoute mockIdentifiableRoute = new IdentifiableRoute(mockRoute, "foobar");
    MapboxNavigation mockedNavigation = mock(MapboxNavigation.class);
    MapView mockedMapView = mock(MapView.class);
    MapboxMap mockedMapboxMap = mock(MapboxMap.class);
    int mockedStyleRes = 0;
    MapRouteClickListener mockedMapClickListener = mock(MapRouteClickListener.class);
    MapView.OnDidFinishLoadingStyleListener mockedDidFinishLoadingStyleListener =
            mock(MapView.OnDidFinishLoadingStyleListener.class);
    MapRouteProgressChangeListener mockedProgressChangeListener = mock(MapRouteProgressChangeListener.class);
    MapRouteLine mockedMapRouteLine = mock(MapRouteLine.class);
    MapRouteArrow mockedMapRouteArrow = mock(MapRouteArrow.class);
    LifecycleOwner mockedLifecycleOwner = mock(LifecycleOwner.class);
    List<IdentifiableRoute> routes = Collections.singletonList(mockIdentifiableRoute);
    NavigationMapRoute theNavigationMapRoute = new NavigationMapRoute(mockedNavigation, mockedMapView, mockedMapboxMap,
            mockedLifecycleOwner, mockedStyleRes, "", mockedMapClickListener, mockedDidFinishLoadingStyleListener,
            mockedProgressChangeListener, mockedMapRouteLine, mockedMapRouteArrow);
    when(mockedMapRouteLine.retrieveDirectionsRoutes()).thenReturn(Collections.singletonList(anotherMockRoute));

    theNavigationMapRoute.addIdentifiableRoutes(routes);

    verify(mockedMapRouteLine).drawIdentifiableRoutes(eq(routes));
  }

  @Test
  public void addIdentifiableRoutes_updatePrimaryRouteIndexWhenSameRoutes() {
    DirectionsRoute mockRoute = mock(DirectionsRoute.class);
    DirectionsRoute anotherMockRoute = mock(DirectionsRoute.class);
    IdentifiableRoute mockIdentifiableRoute = new IdentifiableRoute(mockRoute, "foobar");
    IdentifiableRoute anotherMockIdentifiableRoute = new IdentifiableRoute(anotherMockRoute, "potHoleRoad");
    MapboxNavigation mockedNavigation = mock(MapboxNavigation.class);
    MapView mockedMapView = mock(MapView.class);
    MapboxMap mockedMapboxMap = mock(MapboxMap.class);
    int mockedStyleRes = 0;
    MapRouteClickListener mockedMapClickListener = mock(MapRouteClickListener.class);
    MapView.OnDidFinishLoadingStyleListener mockedDidFinishLoadingStyleListener =
            mock(MapView.OnDidFinishLoadingStyleListener.class);
    MapRouteProgressChangeListener mockedProgressChangeListener = mock(MapRouteProgressChangeListener.class);
    MapRouteLine mockedMapRouteLine = mock(MapRouteLine.class);
    MapRouteArrow mockedMapRouteArrow = mock(MapRouteArrow.class);
    LifecycleOwner mockedLifecycleOwner = mock(LifecycleOwner.class);
    List<IdentifiableRoute> routes = Arrays.asList(anotherMockIdentifiableRoute, mockIdentifiableRoute);
    NavigationMapRoute theNavigationMapRoute = new NavigationMapRoute(mockedNavigation, mockedMapView, mockedMapboxMap,
            mockedLifecycleOwner, mockedStyleRes, "", mockedMapClickListener, mockedDidFinishLoadingStyleListener,
            mockedProgressChangeListener, mockedMapRouteLine, mockedMapRouteArrow);
    when(mockedMapRouteLine.retrieveDirectionsRoutes()).thenReturn(Arrays.asList(mockRoute, anotherMockRoute));

    theNavigationMapRoute.addIdentifiableRoutes(routes);

    verify(mockedMapRouteLine).updatePrimaryRouteIndex(routes.get(0).getRoute());
  }

  @Test
  public void removeRoute_routeLineVisibilityIsUpdated() {
    MapboxNavigation mockedNavigation = mock(MapboxNavigation.class);
    MapView mockedMapView = mock(MapView.class);
    MapboxMap mockedMapboxMap = mock(MapboxMap.class);
    int mockedStyleRes = 0;
    MapRouteClickListener mockedMapClickListener = mock(MapRouteClickListener.class);
    MapView.OnDidFinishLoadingStyleListener mockedDidFinishLoadingStyleListener =
      mock(MapView.OnDidFinishLoadingStyleListener.class);
    MapRouteProgressChangeListener mockedProgressChangeListener = mock(MapRouteProgressChangeListener.class);
    MapRouteLine mockedMapRouteLine = mock(MapRouteLine.class);
    MapRouteArrow mockedMapRouteArrow = mock(MapRouteArrow.class);
    LifecycleOwner mockedLifecycleOwner = mock(LifecycleOwner.class);
    NavigationMapRoute theNavigationMapRoute = new NavigationMapRoute(mockedNavigation, mockedMapView, mockedMapboxMap,
            mockedLifecycleOwner, mockedStyleRes, "", mockedMapClickListener, mockedDidFinishLoadingStyleListener,
            mockedProgressChangeListener, mockedMapRouteLine, mockedMapRouteArrow);

    theNavigationMapRoute.updateRouteVisibilityTo(false);

    verify(mockedMapRouteLine).updateVisibilityTo(false);
  }

  @Test
  public void removeRoute_routeArrowVisibilityIsUpdated() {
    MapboxNavigation mockedNavigation = mock(MapboxNavigation.class);
    MapView mockedMapView = mock(MapView.class);
    MapboxMap mockedMapboxMap = mock(MapboxMap.class);
    int mockedStyleRes = 0;
    MapRouteClickListener mockedMapClickListener = mock(MapRouteClickListener.class);
    MapView.OnDidFinishLoadingStyleListener mockedDidFinishLoadingStyleListener =
      mock(MapView.OnDidFinishLoadingStyleListener.class);
    MapRouteProgressChangeListener mockedProgressChangeListener = mock(MapRouteProgressChangeListener.class);
    MapRouteLine mockedMapRouteLine = mock(MapRouteLine.class);
    MapRouteArrow mockedMapRouteArrow = mock(MapRouteArrow.class);
    LifecycleOwner mockedLifecycleOwner = mock(LifecycleOwner.class);
    NavigationMapRoute theNavigationMapRoute = new NavigationMapRoute(mockedNavigation, mockedMapView, mockedMapboxMap,
            mockedLifecycleOwner, mockedStyleRes, "", mockedMapClickListener, mockedDidFinishLoadingStyleListener,
            mockedProgressChangeListener, mockedMapRouteLine, mockedMapRouteArrow);

    theNavigationMapRoute.updateRouteArrowVisibilityTo(false);

    verify(mockedMapRouteArrow).updateVisibilityTo(false);
  }

  @Test
  public void updateRouteArrowVisibilityTo_routeArrowReceivesNewVisibility() {
    MapboxNavigation mockedNavigation = mock(MapboxNavigation.class);
    MapView mockedMapView = mock(MapView.class);
    MapboxMap mockedMapboxMap = mock(MapboxMap.class);
    int mockedStyleRes = 0;
    MapRouteClickListener mockedMapClickListener = mock(MapRouteClickListener.class);
    MapView.OnDidFinishLoadingStyleListener mockedDidFinishLoadingStyleListener =
      mock(MapView.OnDidFinishLoadingStyleListener.class);
    MapRouteProgressChangeListener mockedProgressChangeListener = mock(MapRouteProgressChangeListener.class);
    MapRouteLine mockedMapRouteLine = mock(MapRouteLine.class);
    MapRouteArrow mockedMapRouteArrow = mock(MapRouteArrow.class);
    LifecycleOwner mockedLifecycleOwner = mock(LifecycleOwner.class);
    NavigationMapRoute theNavigationMapRoute = new NavigationMapRoute(mockedNavigation, mockedMapView, mockedMapboxMap,
            mockedLifecycleOwner, mockedStyleRes, "", mockedMapClickListener, mockedDidFinishLoadingStyleListener,
            mockedProgressChangeListener, mockedMapRouteLine, mockedMapRouteArrow);
    boolean isVisible = false;

    theNavigationMapRoute.updateRouteArrowVisibilityTo(isVisible);

    verify(mockedMapRouteArrow).updateVisibilityTo(isVisible);
  }

  @Test
  public void showAlternativeRoutes_mapRouteProgressChangeListenerIsAdded() {
    MapboxNavigation mockedNavigation = mock(MapboxNavigation.class);
    MapView mockedMapView = mock(MapView.class);
    MapboxMap mockedMapboxMap = mock(MapboxMap.class);
    int mockedStyleRes = 0;
    MapRouteClickListener mockedMapClickListener = mock(MapRouteClickListener.class);
    MapView.OnDidFinishLoadingStyleListener mockedDidFinishLoadingStyleListener =
      mock(MapView.OnDidFinishLoadingStyleListener.class);
    MapRouteProgressChangeListener mockedProgressChangeListener = mock(MapRouteProgressChangeListener.class);
    MapRouteLine mockedMapRouteLine = mock(MapRouteLine.class);
    MapRouteArrow mockedMapRouteArrow = mock(MapRouteArrow.class);
    LifecycleOwner mockedLifecycleOwner = mock(LifecycleOwner.class);
    NavigationMapRoute theNavigationMapRoute = new NavigationMapRoute(mockedNavigation, mockedMapView, mockedMapboxMap,
            mockedLifecycleOwner, mockedStyleRes, "", mockedMapClickListener, mockedDidFinishLoadingStyleListener,
            mockedProgressChangeListener, mockedMapRouteLine, mockedMapRouteArrow);
    boolean isVisible = false;

    theNavigationMapRoute.showAlternativeRoutes(isVisible);

    verify(mockedMapRouteLine).toggleAlternativeVisibilityWith(isVisible);
  }

  @Test
  public void onStartCreatesNewProgressChangeListener() {
    MapboxNavigation mockedNavigation = mock(MapboxNavigation.class);
    MapView mockedMapView = mock(MapView.class);
    MapboxMap mockedMapboxMap = mock(MapboxMap.class);
    int mockedStyleRes = 0;
    MapRouteClickListener mockedMapClickListener = mock(MapRouteClickListener.class);
    MapView.OnDidFinishLoadingStyleListener mockedDidFinishLoadingStyleListener =
            mock(MapView.OnDidFinishLoadingStyleListener.class);
    MapRouteProgressChangeListener mockedProgressChangeListener = mock(MapRouteProgressChangeListener.class);
    LifecycleOwner mockedLifecycleOwner = mock(LifecycleOwner.class);
    MapRouteLine mockedMapRouteLine = mock(MapRouteLine.class);
    MapRouteArrow mockedMapRouteArrow = mock(MapRouteArrow.class);
    NavigationMapRoute theNavigationMapRoute = new NavigationMapRoute(mockedNavigation, mockedMapView, mockedMapboxMap,
            mockedLifecycleOwner, mockedStyleRes, "", mockedMapClickListener, mockedDidFinishLoadingStyleListener,
            mockedProgressChangeListener, mockedMapRouteLine, mockedMapRouteArrow);

    theNavigationMapRoute.onStart();
    theNavigationMapRoute.onNewRouteProgress(mock(RouteProgress.class, RETURNS_DEEP_STUBS));

    verify(mockedProgressChangeListener, never()).onRouteProgressChanged(any());
  }
}
