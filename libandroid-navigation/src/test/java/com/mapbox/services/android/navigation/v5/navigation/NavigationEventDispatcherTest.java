package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;

import com.mapbox.services.android.navigation.BuildConfig;
import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener;
import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = Config.DEFAULT_MANIFEST_NAME)
public class NavigationEventDispatcherTest extends BaseTest {

  @Mock
  MilestoneEventListener milestoneEventListener;
  @Mock
  ProgressChangeListener progressChangeListener;
  @Mock
  OffRouteListener offRouteListener;
  @Mock
  NavigationEventListener navigationEventListener;
  @Mock
  RouteProgress routeProgress;
  @Mock
  Location location;

  private NavigationEventDispatcher navigationEventDispatcher;
  private MapboxNavigation navigation;

  //  @Before
  //  public void setUp() throws Exception {
  //    MockitoAnnotations.initMocks(this);
  //    navigation = new MapboxNavigation(RuntimeEnvironment.application.getApplicationContext(),
  //      "PK.XXX");
  //    navigationEventDispatcher = navigation.getEventDispatcher();
  //  }

  @Test
  public void sanity() throws Exception {
    NavigationEventDispatcher navigationEventDispatcher = new NavigationEventDispatcher();
    assertNotNull(navigationEventDispatcher);
  }

  //  @Test
  //  public void addMilestoneEventListener_didAddListener() throws Exception {
  //    navigationEventDispatcher.onMilestoneEvent(routeProgress, "", 0);
  //    verify(milestoneEventListener, times(0)).onMilestoneEvent(routeProgress, "", 0);
  //
  //    navigation.addMilestoneEventListener(milestoneEventListener);
  //    navigationEventDispatcher.onMilestoneEvent(routeProgress, "", 0);
  //    verify(milestoneEventListener, times(1)).onMilestoneEvent(routeProgress, "", 0);
  //  }
  //
  //  @Test
  //  public void addMilestoneEventListener_onlyAddsListenerOnce() throws Exception {
  //    navigationEventDispatcher.onMilestoneEvent(routeProgress, "", 0);
  //    verify(milestoneEventListener, times(0)).onMilestoneEvent(routeProgress, "", 0);
  //
  //    navigation.addMilestoneEventListener(milestoneEventListener);
  //    navigation.addMilestoneEventListener(milestoneEventListener);
  //    navigation.addMilestoneEventListener(milestoneEventListener);
  //    navigationEventDispatcher.onMilestoneEvent(routeProgress, "", 0);
  //    verify(milestoneEventListener, times(1)).onMilestoneEvent(routeProgress, "", 0);
  //  }
  //
  //  @Test
  //  public void removeMilestoneEventListener_didRemoveListener() throws Exception {
  //    navigation.addMilestoneEventListener(milestoneEventListener);
  //    navigation.removeMilestoneEventListener(milestoneEventListener);
  //    navigationEventDispatcher.onMilestoneEvent(routeProgress, "", 0);
  //    verify(milestoneEventListener, times(0)).onMilestoneEvent(routeProgress, "", 0);
  //  }
  //
  //  @Test
  //  public void removeMilestoneEventListener_nullRemovesAllListeners() throws Exception {
  //    navigation.addMilestoneEventListener(milestoneEventListener);
  //    navigation.addMilestoneEventListener(mock(MilestoneEventListener.class));
  //    navigation.addMilestoneEventListener(mock(MilestoneEventListener.class));
  //    navigation.addMilestoneEventListener(mock(MilestoneEventListener.class));
  //
  //    navigation.removeMilestoneEventListener(null);
  //    navigationEventDispatcher.onMilestoneEvent(routeProgress, "", 0);
  //    verify(milestoneEventListener, times(0)).onMilestoneEvent(routeProgress, "", 0);
  //  }
  //
  //  @Test
  //  public void addProgressChangeListener_didAddListener() throws Exception {
  //    navigationEventDispatcher.onProgressChange(location, routeProgress);
  //    verify(progressChangeListener, times(0)).onProgressChange(location, routeProgress);
  //
  //    navigation.addProgressChangeListener(progressChangeListener);
  //    navigationEventDispatcher.onProgressChange(location, routeProgress);
  //    verify(progressChangeListener, times(1)).onProgressChange(location, routeProgress);
  //  }
  //
  //  @Test
  //  public void addProgressChangeListener_onlyAddsListenerOnce() throws Exception {
  //    navigationEventDispatcher.onProgressChange(location, routeProgress);
  //    verify(progressChangeListener, times(0)).onProgressChange(location, routeProgress);
  //
  //    navigation.addProgressChangeListener(progressChangeListener);
  //    navigation.addProgressChangeListener(progressChangeListener);
  //    navigation.addProgressChangeListener(progressChangeListener);
  //    navigationEventDispatcher.onProgressChange(location, routeProgress);
  //    verify(progressChangeListener, times(1)).onProgressChange(location, routeProgress);
  //  }
  //
  //  @Test
  //  public void removeProgressChangeListener_didRemoveListener() throws Exception {
  //    navigation.addProgressChangeListener(progressChangeListener);
  //    navigation.removeProgressChangeListener(progressChangeListener);
  //    navigationEventDispatcher.onProgressChange(location, routeProgress);
  //    verify(progressChangeListener, times(0)).onProgressChange(location, routeProgress);
  //  }
  //
  //  @Test
  //  public void removeProgressChangeListener_nullRemovesAllListeners() throws Exception {
  //    navigation.addProgressChangeListener(progressChangeListener);
  //    navigation.addProgressChangeListener(mock(ProgressChangeListener.class));
  //    navigation.addProgressChangeListener(mock(ProgressChangeListener.class));
  //    navigation.addProgressChangeListener(mock(ProgressChangeListener.class));
  //
  //    navigation.removeProgressChangeListener(null);
  //    navigationEventDispatcher.onProgressChange(location, routeProgress);
  //    verify(progressChangeListener, times(0)).onProgressChange(location, routeProgress);
  //  }
  //
  //  @Test
  //  public void addOffRouteListener_didAddListener() throws Exception {
  //    navigationEventDispatcher.onUserOffRoute(location);
  //    verify(offRouteListener, times(0)).userOffRoute(location);
  //
  //    navigation.addOffRouteListener(offRouteListener);
  //    navigationEventDispatcher.onUserOffRoute(location);
  //    verify(offRouteListener, times(1)).userOffRoute(location);
  //  }
  //
  //  @Test
  //  public void addOffRouteListener_onlyAddsListenerOnce() throws Exception {
  //    navigationEventDispatcher.onUserOffRoute(location);
  //    verify(offRouteListener, times(0)).userOffRoute(location);
  //
  //    navigation.addOffRouteListener(offRouteListener);
  //    navigation.addOffRouteListener(offRouteListener);
  //    navigation.addOffRouteListener(offRouteListener);
  //    navigationEventDispatcher.onUserOffRoute(location);
  //    verify(offRouteListener, times(1)).userOffRoute(location);
  //  }
  //
  //  @Test
  //  public void removeOffRouteListener_didRemoveListener() throws Exception {
  //    navigation.addOffRouteListener(offRouteListener);
  //    navigation.removeOffRouteListener(offRouteListener);
  //    navigationEventDispatcher.onUserOffRoute(location);
  //    verify(offRouteListener, times(0)).userOffRoute(location);
  //  }
  //
  //  @Test
  //  public void removeOffRouteListener_nullRemovesAllListeners() throws Exception {
  //    navigation.addOffRouteListener(offRouteListener);
  //    navigation.addOffRouteListener(mock(OffRouteListener.class));
  //    navigation.addOffRouteListener(mock(OffRouteListener.class));
  //    navigation.addOffRouteListener(mock(OffRouteListener.class));
  //    navigation.addOffRouteListener(mock(OffRouteListener.class));
  //
  //    navigation.removeOffRouteListener(null);
  //    navigationEventDispatcher.onUserOffRoute(location);
  //    verify(offRouteListener, times(0)).userOffRoute(location);
  //  }
  //
  //  @Test
  //  public void addNavigationEventListener_didAddListener() throws Exception {
  //    navigationEventDispatcher.onNavigationEvent(true);
  //    verify(navigationEventListener, times(0)).onRunning(true);
  //
  //    navigation.addNavigationEventListener(navigationEventListener);
  //    navigationEventDispatcher.onNavigationEvent(true);
  //    verify(navigationEventListener, times(1)).onRunning(true);
  //  }
  //
  //  @Test
  //  public void addNavigationEventListener_onlyAddsListenerOnce() throws Exception {
  //    navigationEventDispatcher.onNavigationEvent(true);
  //    verify(navigationEventListener, times(0)).onRunning(true);
  //
  //    navigation.addNavigationEventListener(navigationEventListener);
  //    navigation.addNavigationEventListener(navigationEventListener);
  //    navigation.addNavigationEventListener(navigationEventListener);
  //    navigationEventDispatcher.onNavigationEvent(true);
  //    verify(navigationEventListener, times(1)).onRunning(true);
  //  }
  //
  //  @Test
  //  public void removeNavigationEventListener_didRemoveListener() throws Exception {
  //    navigation.addNavigationEventListener(navigationEventListener);
  //    navigation.removeNavigationEventListener(navigationEventListener);
  //    navigationEventDispatcher.onNavigationEvent(true);
  //    verify(navigationEventListener, times(0)).onRunning(true);
  //  }
  //
  //  @Test
  //  public void removeNavigationEventListener_nullRemovesAllListeners() throws Exception {
  //    navigation.addNavigationEventListener(navigationEventListener);
  //    navigation.addNavigationEventListener(mock(NavigationEventListener.class));
  //    navigation.addNavigationEventListener(mock(NavigationEventListener.class));
  //    navigation.addNavigationEventListener(mock(NavigationEventListener.class));
  //    navigation.addNavigationEventListener(mock(NavigationEventListener.class));
  //
  //    navigation.removeNavigationEventListener(null);
  //    navigationEventDispatcher.onNavigationEvent(true);
  //    verify(navigationEventListener, times(0)).onRunning(true);
  //  }
  //
  //  @Test
  //  public void setInternalProgressChangeListener_didGetSet() throws Exception {
  //    navigationEventDispatcher.setInternalProgressChangeListener(progressChangeListener);
  //    when(routeProgress.fractionTraveled())
  //      .thenReturn(1f);
  //    navigationEventDispatcher.onProgressChange(location, routeProgress);
  //    verify(progressChangeListener, times(1)).onProgressChange(location, routeProgress);
  //  }
  //
  //  @Test
  //  public void setInternalProgressChangeListener_didNotGetTriggeredUntilArrival() throws Exception {
  //    navigationEventDispatcher.setInternalProgressChangeListener(progressChangeListener);
  //    when(routeProgress.fractionTraveled())
  //      .thenReturn(0.5f)
  //      .thenReturn(1f);
  //
  //    navigationEventDispatcher.onProgressChange(location, routeProgress);
  //    verify(progressChangeListener, times(0)).onProgressChange(location, routeProgress);
  //    navigationEventDispatcher.onProgressChange(location, routeProgress);
  //    verify(progressChangeListener, times(1)).onProgressChange(location, routeProgress);
  //  }
}
