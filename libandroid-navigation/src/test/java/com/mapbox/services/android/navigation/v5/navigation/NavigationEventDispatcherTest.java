package com.mapbox.services.android.navigation.v5.navigation;

import android.content.Context;
import android.location.Location;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mapbox.api.directions.v5.DirectionsAdapterFactory;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.services.android.navigation.BuildConfig;
import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.services.android.navigation.v5.milestone.Milestone;
import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener;
import com.mapbox.services.android.navigation.v5.navigation.metrics.NavigationMetricListeners;
import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener;
import com.mapbox.services.android.navigation.v5.route.FasterRouteListener;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.telemetry.location.LocationEngine;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = Config.DEFAULT_MANIFEST_NAME)
public class NavigationEventDispatcherTest extends BaseTest {

  private static final String PRECISION_6 = "directions_v5_precision_6.json";

  @Mock
  MilestoneEventListener milestoneEventListener;
  @Mock
  ProgressChangeListener progressChangeListener;
  @Mock
  NavigationMetricListeners.EventListeners eventListeners;
  @Mock
  NavigationMetricListeners.ArrivalListener arrivalListener;
  @Mock
  OffRouteListener offRouteListener;
  @Mock
  NavigationEventListener navigationEventListener;
  @Mock
  FasterRouteListener fasterRouteListener;
  @Mock
  Location location;
  @Mock
  Milestone milestone;

  private NavigationEventDispatcher navigationEventDispatcher;
  private MapboxNavigation navigation;
  private DirectionsRoute route;
  private RouteProgress routeProgress;

  @Before
  public void setup() throws IOException {
    MockitoAnnotations.initMocks(this);
    navigation = new MapboxNavigation(mock(Context.class), ACCESS_TOKEN, mock(NavigationTelemetry.class),
      mock(LocationEngine.class));
    navigationEventDispatcher = navigation.getEventDispatcher();

    Gson gson = new GsonBuilder()
      .registerTypeAdapterFactory(DirectionsAdapterFactory.create()).create();
    String body = loadJsonFixture(PRECISION_6);
    DirectionsResponse response = gson.fromJson(body, DirectionsResponse.class);
    route = response.routes().get(0);
    routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(100)
      .legDistanceRemaining(100)
      .distanceRemaining(100)
      .directionsRoute(route)
      .stepIndex(0)
      .legIndex(0)
      .build();
  }

  @Test
  public void sanity() throws Exception {
    NavigationEventDispatcher navigationEventDispatcher = new NavigationEventDispatcher();
    assertNotNull(navigationEventDispatcher);
  }

  @Test
  public void addMilestoneEventListener_didAddListener() throws Exception {
    navigationEventDispatcher.onMilestoneEvent(routeProgress, "", milestone);
    verify(milestoneEventListener, times(0)).onMilestoneEvent(routeProgress, "", milestone);

    navigation.addMilestoneEventListener(milestoneEventListener);
    navigationEventDispatcher.onMilestoneEvent(routeProgress, "", milestone);
    verify(milestoneEventListener, times(1)).onMilestoneEvent(routeProgress, "", milestone);
  }

  @Test
  public void addMilestoneEventListener_onlyAddsListenerOnce() throws Exception {
    navigationEventDispatcher.onMilestoneEvent(routeProgress, "", milestone);
    verify(milestoneEventListener, times(0)).onMilestoneEvent(routeProgress, "", milestone);

    navigation.addMilestoneEventListener(milestoneEventListener);
    navigation.addMilestoneEventListener(milestoneEventListener);
    navigation.addMilestoneEventListener(milestoneEventListener);
    navigationEventDispatcher.onMilestoneEvent(routeProgress, "", milestone);
    verify(milestoneEventListener, times(1)).onMilestoneEvent(routeProgress, "", milestone);
  }

  @Test
  public void removeMilestoneEventListener_didRemoveListener() throws Exception {
    navigation.addMilestoneEventListener(milestoneEventListener);
    navigation.removeMilestoneEventListener(milestoneEventListener);
    navigationEventDispatcher.onMilestoneEvent(routeProgress, "", milestone);
    verify(milestoneEventListener, times(0)).onMilestoneEvent(routeProgress, "", milestone);
  }

  @Test
  public void removeMilestoneEventListener_nullRemovesAllListeners() throws Exception {
    navigation.addMilestoneEventListener(milestoneEventListener);
    navigation.addMilestoneEventListener(mock(MilestoneEventListener.class));
    navigation.addMilestoneEventListener(mock(MilestoneEventListener.class));
    navigation.addMilestoneEventListener(mock(MilestoneEventListener.class));

    navigation.removeMilestoneEventListener(null);
    navigationEventDispatcher.onMilestoneEvent(routeProgress, "", milestone);
    verify(milestoneEventListener, times(0)).onMilestoneEvent(routeProgress, "", milestone);
  }

  @Test
  public void addProgressChangeListener_didAddListener() throws Exception {
    navigationEventDispatcher.onProgressChange(location, routeProgress);
    verify(progressChangeListener, times(0)).onProgressChange(location, routeProgress);

    navigation.addProgressChangeListener(progressChangeListener);
    navigationEventDispatcher.onProgressChange(location, routeProgress);
    verify(progressChangeListener, times(1)).onProgressChange(location, routeProgress);
  }

  @Test
  public void addProgressChangeListener_onlyAddsListenerOnce() throws Exception {
    navigationEventDispatcher.onProgressChange(location, routeProgress);
    verify(progressChangeListener, times(0)).onProgressChange(location, routeProgress);

    navigation.addProgressChangeListener(progressChangeListener);
    navigation.addProgressChangeListener(progressChangeListener);
    navigation.addProgressChangeListener(progressChangeListener);
    navigationEventDispatcher.onProgressChange(location, routeProgress);
    verify(progressChangeListener, times(1)).onProgressChange(location, routeProgress);
  }

  @Test
  public void removeProgressChangeListener_didRemoveListener() throws Exception {
    navigation.addProgressChangeListener(progressChangeListener);
    navigation.removeProgressChangeListener(progressChangeListener);
    navigationEventDispatcher.onProgressChange(location, routeProgress);
    verify(progressChangeListener, times(0)).onProgressChange(location, routeProgress);
  }

  @Test
  public void removeProgressChangeListener_nullRemovesAllListeners() throws Exception {
    navigation.addProgressChangeListener(progressChangeListener);
    navigation.addProgressChangeListener(mock(ProgressChangeListener.class));
    navigation.addProgressChangeListener(mock(ProgressChangeListener.class));
    navigation.addProgressChangeListener(mock(ProgressChangeListener.class));

    navigation.removeProgressChangeListener(null);
    navigationEventDispatcher.onProgressChange(location, routeProgress);
    verify(progressChangeListener, times(0)).onProgressChange(location, routeProgress);
  }

  @Test
  public void addOffRouteListener_didAddListener() throws Exception {
    navigationEventDispatcher.onUserOffRoute(location);
    verify(offRouteListener, times(0)).userOffRoute(location);

    navigation.addOffRouteListener(offRouteListener);
    navigationEventDispatcher.onUserOffRoute(location);
    verify(offRouteListener, times(1)).userOffRoute(location);
  }

  @Test
  public void addOffRouteListener_onlyAddsListenerOnce() throws Exception {
    navigationEventDispatcher.onUserOffRoute(location);
    verify(offRouteListener, times(0)).userOffRoute(location);

    navigation.addOffRouteListener(offRouteListener);
    navigation.addOffRouteListener(offRouteListener);
    navigation.addOffRouteListener(offRouteListener);
    navigationEventDispatcher.onUserOffRoute(location);
    verify(offRouteListener, times(1)).userOffRoute(location);
  }

  @Test
  public void removeOffRouteListener_didRemoveListener() throws Exception {
    navigation.addOffRouteListener(offRouteListener);
    navigation.removeOffRouteListener(offRouteListener);
    navigationEventDispatcher.onUserOffRoute(location);
    verify(offRouteListener, times(0)).userOffRoute(location);
  }

  @Test
  public void removeOffRouteListener_nullRemovesAllListeners() throws Exception {
    navigation.addOffRouteListener(offRouteListener);
    navigation.addOffRouteListener(mock(OffRouteListener.class));
    navigation.addOffRouteListener(mock(OffRouteListener.class));
    navigation.addOffRouteListener(mock(OffRouteListener.class));
    navigation.addOffRouteListener(mock(OffRouteListener.class));

    navigation.removeOffRouteListener(null);
    navigationEventDispatcher.onUserOffRoute(location);
    verify(offRouteListener, times(0)).userOffRoute(location);
  }

  @Test
  public void addNavigationEventListener_didAddListener() throws Exception {
    navigationEventDispatcher.onNavigationEvent(true);
    verify(navigationEventListener, times(0)).onRunning(true);

    navigation.addNavigationEventListener(navigationEventListener);
    navigationEventDispatcher.onNavigationEvent(true);
    verify(navigationEventListener, times(1)).onRunning(true);
  }

  @Test
  public void addNavigationEventListener_onlyAddsListenerOnce() throws Exception {
    navigationEventDispatcher.onNavigationEvent(true);
    verify(navigationEventListener, times(0)).onRunning(true);

    navigation.addNavigationEventListener(navigationEventListener);
    navigation.addNavigationEventListener(navigationEventListener);
    navigation.addNavigationEventListener(navigationEventListener);
    navigationEventDispatcher.onNavigationEvent(true);
    verify(navigationEventListener, times(1)).onRunning(true);
  }

  @Test
  public void removeNavigationEventListener_didRemoveListener() throws Exception {
    navigation.addNavigationEventListener(navigationEventListener);
    navigation.removeNavigationEventListener(navigationEventListener);
    navigationEventDispatcher.onNavigationEvent(true);
    verify(navigationEventListener, times(0)).onRunning(true);
  }

  @Test
  public void removeNavigationEventListener_nullRemovesAllListeners() throws Exception {
    navigation.addNavigationEventListener(navigationEventListener);
    navigation.addNavigationEventListener(mock(NavigationEventListener.class));
    navigation.addNavigationEventListener(mock(NavigationEventListener.class));
    navigation.addNavigationEventListener(mock(NavigationEventListener.class));
    navigation.addNavigationEventListener(mock(NavigationEventListener.class));

    navigation.removeNavigationEventListener(null);
    navigationEventDispatcher.onNavigationEvent(true);
    verify(navigationEventListener, times(0)).onRunning(true);
  }

  @Test
  public void addFasterRouteListener_didAddListener() throws Exception {
    navigationEventDispatcher.onFasterRouteEvent(route);
    verify(fasterRouteListener, times(0)).fasterRouteFound(route);

    navigation.addFasterRouteListener(fasterRouteListener);
    navigationEventDispatcher.onFasterRouteEvent(route);
    verify(fasterRouteListener, times(1)).fasterRouteFound(route);
  }

  @Test
  public void addFasterRouteListener_onlyAddsListenerOnce() throws Exception {
    navigationEventDispatcher.onFasterRouteEvent(route);
    verify(fasterRouteListener, times(0)).fasterRouteFound(route);

    navigation.addFasterRouteListener(fasterRouteListener);
    navigation.addFasterRouteListener(fasterRouteListener);
    navigation.addFasterRouteListener(fasterRouteListener);
    navigationEventDispatcher.onFasterRouteEvent(route);
    verify(fasterRouteListener, times(1)).fasterRouteFound(route);
  }

  @Test
  public void removeFasterRouteListener_didRemoveListener() throws Exception {
    navigation.addFasterRouteListener(fasterRouteListener);
    navigation.removeFasterRouteListener(fasterRouteListener);
    navigationEventDispatcher.onFasterRouteEvent(route);
    verify(fasterRouteListener, times(0)).fasterRouteFound(route);
  }

  @Test
  public void removeFasterRouteListener_nullRemovesAllListeners() throws Exception {
    navigation.addFasterRouteListener(fasterRouteListener);
    navigation.addFasterRouteListener(mock(FasterRouteListener.class));
    navigation.addFasterRouteListener(mock(FasterRouteListener.class));
    navigation.addFasterRouteListener(mock(FasterRouteListener.class));
    navigation.addFasterRouteListener(mock(FasterRouteListener.class));

    navigation.removeFasterRouteListener(null);
    navigationEventDispatcher.onFasterRouteEvent(route);
    verify(fasterRouteListener, times(0)).fasterRouteFound(route);
  }


  @Test
  public void setNavigationMetricListener_didGetSet() throws Exception {
    navigationEventDispatcher.addMetricEventListeners(eventListeners);
    navigationEventDispatcher.onProgressChange(location, routeProgress);

    verify(eventListeners, times(1)).onRouteProgressUpdate(routeProgress);
  }

  @Test
  public void setNavigationMetricListener_didNotGetTriggeredUntilArrival() throws Exception {
    RouteLeg lastLeg = route.legs().get(route.legs().size() - 1);
    int lastStepIndex = lastLeg.steps().indexOf(lastLeg.steps().get(lastLeg.steps().size() - 1));

    navigationEventDispatcher.addMetricEventListeners(eventListeners);
    navigationEventDispatcher.addMetricArrivalListener(arrivalListener);

    // Progress that hasn't arrived
    RouteProgress routeProgressDidNotArrive = RouteProgress.builder()
      .stepDistanceRemaining(100)
      .legDistanceRemaining(100)
      .distanceRemaining(100)
      .directionsRoute(route)
      .stepIndex(lastStepIndex)
      .legIndex(0)
      .build();

    navigationEventDispatcher.onProgressChange(location, routeProgressDidNotArrive);
    verify(eventListeners, times(1)).onRouteProgressUpdate(routeProgressDidNotArrive);
    verify(arrivalListener, times(0)).onArrival(location, routeProgressDidNotArrive);

    // Progress that has arrived
    RouteProgress routeProgressDidArrive = RouteProgress.builder()
      .stepDistanceRemaining(30)
      .legDistanceRemaining(30)
      .distanceRemaining(30)
      .directionsRoute(route)
      .stepIndex(lastStepIndex)
      .legIndex(0)
      .build();

    navigationEventDispatcher.onProgressChange(location, routeProgressDidArrive);
    verify(eventListeners, times(1)).onRouteProgressUpdate(routeProgressDidArrive);
    verify(arrivalListener, times(1)).onArrival(location, routeProgressDidArrive);
  }
}
