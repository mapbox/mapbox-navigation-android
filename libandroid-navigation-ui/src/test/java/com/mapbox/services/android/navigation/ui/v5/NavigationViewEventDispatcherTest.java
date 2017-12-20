package com.mapbox.services.android.navigation.ui.v5;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.ui.v5.feedback.FeedbackItem;
import com.mapbox.services.android.navigation.ui.v5.listeners.FeedbackListener;
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener;
import com.mapbox.services.android.navigation.ui.v5.listeners.RouteListener;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
@Config(constants = com.mapbox.services.android.navigation.BuildConfig.class, manifest = Config.DEFAULT_MANIFEST_NAME)
public class NavigationViewEventDispatcherTest {

  @Mock
  NavigationListener navigationListener;

  @Mock
  RouteListener routeListener;

  @Mock
  FeedbackListener feedbackListener;

  @Mock
  Point point;

  @Mock
  DirectionsRoute directionsRoute;

  private NavigationViewEventDispatcher eventDispatcher;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    eventDispatcher = new NavigationViewEventDispatcher();
  }

  @Test
  public void sanity() throws Exception {
    assertNotNull(eventDispatcher);
  }

  @Test
  public void setNavigationListener_cancelListenerIsCalled() throws Exception {
    eventDispatcher.setNavigationListener(navigationListener);
    eventDispatcher.onCancelNavigation();
    verify(navigationListener, times(1)).onCancelNavigation();
  }

  @Test
  public void setNavigationListener_finishedListenerIsCalled() throws Exception {
    eventDispatcher.setNavigationListener(navigationListener);
    eventDispatcher.onNavigationFinished();
    verify(navigationListener, times(1)).onNavigationFinished();
  }

  @Test
  public void setNavigationListener_runningListenerIsCalled() throws Exception {
    eventDispatcher.setNavigationListener(navigationListener);
    eventDispatcher.onNavigationRunning();
    verify(navigationListener, times(1)).onNavigationRunning();
  }

  @Test
  public void onNavigationListenerNotSet_runningListenerIsNotCalled() throws Exception {
    eventDispatcher.onNavigationRunning();
    verify(navigationListener, times(0)).onNavigationRunning();
  }

  @Test
  public void onNavigationListenerNotSet_cancelListenerIsNotCalled() throws Exception {
    eventDispatcher.onCancelNavigation();
    verify(navigationListener, times(0)).onCancelNavigation();
  }

  @Test
  public void onNavigationListenerNotSet_finishedListenerIsNotCalled() throws Exception {
    eventDispatcher.onNavigationFinished();
    verify(navigationListener, times(0)).onNavigationFinished();
  }

  @Test
  public void setRouteListener_offRouteListenerIsCalled() throws Exception {
    eventDispatcher.setRouteListener(routeListener);
    eventDispatcher.onOffRoute(point);
    verify(routeListener, times(1)).onOffRoute(point);
  }

  @Test
  public void setRouteListener_rerouteAlongListenerIsCalled() throws Exception {
    eventDispatcher.setRouteListener(routeListener);
    eventDispatcher.onRerouteAlong(directionsRoute);
    verify(routeListener, times(1)).onRerouteAlong(directionsRoute);
  }

  @Test
  public void setRouteListener_failedRerouteListenerIsCalled() throws Exception {
    String errorMessage = "errorMessage";
    eventDispatcher.setRouteListener(routeListener);
    eventDispatcher.onFailedReroute(errorMessage);
    verify(routeListener, times(1)).onFailedReroute(errorMessage);
  }

  @Test
  public void setRouteListener_allowRerouteFromListenerIsCalled() throws Exception {
    eventDispatcher.setRouteListener(routeListener);
    eventDispatcher.allowRerouteFrom(point);
    verify(routeListener, times(1)).allowRerouteFrom(point);
  }

  @Test
  public void onRouteListenerNotSet_allowRerouteFromListenerIsNotCalled_andReturnsTrue() throws Exception {
    boolean shouldAllowReroute = eventDispatcher.allowRerouteFrom(point);
    verify(routeListener, times(0)).allowRerouteFrom(point);
    assertTrue(shouldAllowReroute);
  }

  @Test
  public void onRouteListenerNotSet_offRouteListenerIsNotCalled() throws Exception {
    eventDispatcher.onOffRoute(point);
    verify(routeListener, times(0)).onOffRoute(point);
  }

  @Test
  public void onRouteListenerNotSet_rerouteAlongListenerIsNotCalled() throws Exception {
    eventDispatcher.onRerouteAlong(directionsRoute);
    verify(routeListener, times(0)).onRerouteAlong(directionsRoute);
  }

  @Test
  public void onRouteListenerNotSet_failedListenerIsNotCalled() throws Exception {
    String errorMessage = "errorMessage";
    eventDispatcher.onFailedReroute(errorMessage);
    verify(routeListener, times(0)).onFailedReroute(errorMessage);
  }

  @Test
  public void onRouteListenerNotSet_allowRerouteListenerIsNotCalled() throws Exception {
    eventDispatcher.allowRerouteFrom(point);
    verify(routeListener, times(0)).allowRerouteFrom(point);
  }

  @Test
  public void setFeedbackListener_feedbackOpenIsCalled() throws Exception {
    eventDispatcher.setFeedbackListener(feedbackListener);
    eventDispatcher.onFeedbackOpened();
    verify(feedbackListener, times(1)).onFeedbackOpened();
  }

  @Test
  public void setFeedbackListener_feedbackCancelledIsCalled() throws Exception {
    eventDispatcher.setFeedbackListener(feedbackListener);
    eventDispatcher.onFeedbackCancelled();
    verify(feedbackListener, times(1)).onFeedbackCancelled();
  }

  @Test
  public void setFeedbackListener_feedbackSentIsCalled() throws Exception {
    FeedbackItem item = mock(FeedbackItem.class);
    eventDispatcher.setFeedbackListener(feedbackListener);
    eventDispatcher.onFeedbackSent(item);
    verify(feedbackListener, times(1)).onFeedbackSent(item);
  }

  @Test
  public void onFeedbackListenerNotSet_feedbackOpenedIsNotCalled() throws Exception {
    eventDispatcher.onFeedbackOpened();
    verify(feedbackListener, times(0)).onFeedbackOpened();
  }

  @Test
  public void onFeedbackListenerNotSet_feedbackCancelledIsNotCalled() throws Exception {
    eventDispatcher.onFeedbackOpened();
    verify(feedbackListener, times(0)).onFeedbackCancelled();
  }

  @Test
  public void onFeedbackListenerNotSet_feedbackSentIsNotCalled() throws Exception {
    FeedbackItem item = mock(FeedbackItem.class);
    eventDispatcher.onFeedbackSent(item);
    verify(feedbackListener, times(0)).onFeedbackSent(item);
  }
}