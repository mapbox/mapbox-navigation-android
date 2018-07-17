package com.mapbox.services.android.navigation.ui.v5;

import android.support.annotation.NonNull;

import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.ui.v5.feedback.FeedbackItem;
import com.mapbox.services.android.navigation.ui.v5.listeners.BannerInstructionsListener;
import com.mapbox.services.android.navigation.ui.v5.listeners.FeedbackListener;
import com.mapbox.services.android.navigation.ui.v5.listeners.InstructionListListener;
import com.mapbox.services.android.navigation.ui.v5.listeners.SpeechAnnouncementListener;
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener;
import com.mapbox.services.android.navigation.ui.v5.listeners.RouteListener;
import com.mapbox.services.android.navigation.ui.v5.voice.SpeechAnnouncement;
import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NavigationViewEventDispatcherTest {

  @Test
  public void sanity() throws Exception {
    NavigationViewEventDispatcher eventDispatcher = new NavigationViewEventDispatcher();

    assertNotNull(eventDispatcher);
  }

  @Test
  public void setNavigationListener_cancelListenerIsCalled() throws Exception {
    NavigationViewEventDispatcher eventDispatcher = new NavigationViewEventDispatcher();
    NavigationListener navigationListener = mock(NavigationListener.class);
    eventDispatcher.assignNavigationListener(navigationListener);

    eventDispatcher.onCancelNavigation();

    verify(navigationListener, times(1)).onCancelNavigation();
  }

  @Test
  public void setNavigationListener_finishedListenerIsCalled() throws Exception {
    NavigationViewEventDispatcher eventDispatcher = new NavigationViewEventDispatcher();
    NavigationListener navigationListener = mock(NavigationListener.class);
    eventDispatcher.assignNavigationListener(navigationListener);

    eventDispatcher.onNavigationFinished();

    verify(navigationListener, times(1)).onNavigationFinished();
  }

  @Test
  public void setNavigationListener_runningListenerIsCalled() throws Exception {
    NavigationViewEventDispatcher eventDispatcher = new NavigationViewEventDispatcher();
    NavigationListener navigationListener = mock(NavigationListener.class);
    eventDispatcher.assignNavigationListener(navigationListener);

    eventDispatcher.onNavigationRunning();

    verify(navigationListener, times(1)).onNavigationRunning();
  }

  @Test
  public void onNavigationListenerNotSet_runningListenerIsNotCalled() throws Exception {
    NavigationViewEventDispatcher eventDispatcher = new NavigationViewEventDispatcher();
    NavigationListener navigationListener = mock(NavigationListener.class);

    eventDispatcher.onNavigationRunning();

    verify(navigationListener, times(0)).onNavigationRunning();
  }

  @Test
  public void onNavigationListenerNotSet_cancelListenerIsNotCalled() throws Exception {
    NavigationViewEventDispatcher eventDispatcher = new NavigationViewEventDispatcher();
    NavigationListener navigationListener = mock(NavigationListener.class);

    eventDispatcher.onCancelNavigation();

    verify(navigationListener, times(0)).onCancelNavigation();
  }

  @Test
  public void onNavigationListenerNotSet_finishedListenerIsNotCalled() throws Exception {
    NavigationViewEventDispatcher eventDispatcher = new NavigationViewEventDispatcher();
    NavigationListener navigationListener = mock(NavigationListener.class);

    eventDispatcher.onNavigationFinished();

    verify(navigationListener, times(0)).onNavigationFinished();
  }

  @Test
  public void setRouteListener_offRouteListenerIsCalled() throws Exception {
    NavigationViewEventDispatcher eventDispatcher = new NavigationViewEventDispatcher();
    RouteListener routeListener = mock(RouteListener.class);
    Point point = mock(Point.class);
    eventDispatcher.assignRouteListener(routeListener);

    eventDispatcher.onOffRoute(point);

    verify(routeListener, times(1)).onOffRoute(point);
  }

  @Test
  public void setRouteListener_rerouteAlongListenerIsCalled() throws Exception {
    NavigationViewEventDispatcher eventDispatcher = new NavigationViewEventDispatcher();
    RouteListener routeListener = mock(RouteListener.class);
    DirectionsRoute directionsRoute = mock(DirectionsRoute.class);
    eventDispatcher.assignRouteListener(routeListener);

    eventDispatcher.onRerouteAlong(directionsRoute);

    verify(routeListener, times(1)).onRerouteAlong(directionsRoute);
  }

  @Test
  public void setRouteListener_failedRerouteListenerIsCalled() throws Exception {
    NavigationViewEventDispatcher eventDispatcher = new NavigationViewEventDispatcher();
    RouteListener routeListener = mock(RouteListener.class);
    String errorMessage = "errorMessage";
    eventDispatcher.assignRouteListener(routeListener);

    eventDispatcher.onFailedReroute(errorMessage);

    verify(routeListener, times(1)).onFailedReroute(errorMessage);
  }

  @Test
  public void setRouteListener_allowRerouteFromListenerIsCalled() throws Exception {
    NavigationViewEventDispatcher eventDispatcher = new NavigationViewEventDispatcher();
    RouteListener routeListener = mock(RouteListener.class);
    Point point = mock(Point.class);
    eventDispatcher.assignRouteListener(routeListener);

    eventDispatcher.allowRerouteFrom(point);

    verify(routeListener, times(1)).allowRerouteFrom(point);
  }

  @Test
  public void onRouteListenerNotSet_allowRerouteFromListenerIsNotCalled_andReturnsTrue() throws Exception {
    NavigationViewEventDispatcher eventDispatcher = new NavigationViewEventDispatcher();
    RouteListener routeListener = mock(RouteListener.class);
    Point point = mock(Point.class);

    boolean shouldAllowReroute = eventDispatcher.allowRerouteFrom(point);

    verify(routeListener, times(0)).allowRerouteFrom(point);
    assertTrue(shouldAllowReroute);
  }

  @Test
  public void onRouteListenerNotSet_offRouteListenerIsNotCalled() throws Exception {
    NavigationViewEventDispatcher eventDispatcher = new NavigationViewEventDispatcher();
    RouteListener routeListener = mock(RouteListener.class);
    Point point = mock(Point.class);

    eventDispatcher.onOffRoute(point);

    verify(routeListener, times(0)).onOffRoute(point);
  }

  @Test
  public void onRouteListenerNotSet_rerouteAlongListenerIsNotCalled() throws Exception {
    NavigationViewEventDispatcher eventDispatcher = new NavigationViewEventDispatcher();
    RouteListener routeListener = mock(RouteListener.class);
    DirectionsRoute directionsRoute = mock(DirectionsRoute.class);

    eventDispatcher.onRerouteAlong(directionsRoute);

    verify(routeListener, times(0)).onRerouteAlong(directionsRoute);
  }

  @Test
  public void onRouteListenerNotSet_failedListenerIsNotCalled() throws Exception {
    NavigationViewEventDispatcher eventDispatcher = new NavigationViewEventDispatcher();
    RouteListener routeListener = mock(RouteListener.class);
    String errorMessage = "errorMessage";

    eventDispatcher.onFailedReroute(errorMessage);

    verify(routeListener, times(0)).onFailedReroute(errorMessage);
  }

  @Test
  public void onRouteListenerNotSet_allowRerouteListenerIsNotCalled() throws Exception {
    NavigationViewEventDispatcher eventDispatcher = new NavigationViewEventDispatcher();
    RouteListener routeListener = mock(RouteListener.class);
    Point point = mock(Point.class);

    eventDispatcher.allowRerouteFrom(point);

    verify(routeListener, times(0)).allowRerouteFrom(point);
  }

  @Test
  public void setArrivalListener_arrivalListenerIsCalled() throws Exception {
    NavigationViewEventDispatcher eventDispatcher = new NavigationViewEventDispatcher();
    RouteListener routeListener = mock(RouteListener.class);
    eventDispatcher.assignRouteListener(routeListener);

    eventDispatcher.onArrival();

    verify(routeListener, times(1)).onArrival();
  }

  @Test
  public void onRouteListenerNotSet_arrivalListenerIsNotCalled() throws Exception {
    NavigationViewEventDispatcher eventDispatcher = new NavigationViewEventDispatcher();
    RouteListener routeListener = mock(RouteListener.class);

    eventDispatcher.onArrival();

    verify(routeListener, times(0)).onArrival();
  }

  @Test
  public void setFeedbackListener_feedbackOpenIsCalled() throws Exception {
    NavigationViewEventDispatcher eventDispatcher = new NavigationViewEventDispatcher();
    FeedbackListener feedbackListener = mock(FeedbackListener.class);
    eventDispatcher.assignFeedbackListener(feedbackListener);

    eventDispatcher.onFeedbackOpened();

    verify(feedbackListener, times(1)).onFeedbackOpened();
  }

  @Test
  public void setFeedbackListener_feedbackCancelledIsCalled() throws Exception {
    NavigationViewEventDispatcher eventDispatcher = new NavigationViewEventDispatcher();
    FeedbackListener feedbackListener = mock(FeedbackListener.class);
    eventDispatcher.assignFeedbackListener(feedbackListener);

    eventDispatcher.onFeedbackCancelled();

    verify(feedbackListener, times(1)).onFeedbackCancelled();
  }

  @Test
  public void setFeedbackListener_feedbackSentIsCalled() throws Exception {
    NavigationViewEventDispatcher eventDispatcher = new NavigationViewEventDispatcher();
    FeedbackListener feedbackListener = mock(FeedbackListener.class);
    eventDispatcher.assignFeedbackListener(feedbackListener);
    FeedbackItem item = mock(FeedbackItem.class);
    eventDispatcher.assignFeedbackListener(feedbackListener);

    eventDispatcher.onFeedbackSent(item);

    verify(feedbackListener, times(1)).onFeedbackSent(item);
  }

  @Test
  public void onFeedbackListenerNotSet_feedbackOpenedIsNotCalled() throws Exception {
    NavigationViewEventDispatcher eventDispatcher = new NavigationViewEventDispatcher();
    FeedbackListener feedbackListener = mock(FeedbackListener.class);

    eventDispatcher.onFeedbackOpened();

    verify(feedbackListener, times(0)).onFeedbackOpened();
  }

  @Test
  public void onFeedbackListenerNotSet_feedbackCancelledIsNotCalled() throws Exception {
    NavigationViewEventDispatcher eventDispatcher = new NavigationViewEventDispatcher();
    FeedbackListener feedbackListener = mock(FeedbackListener.class);
    eventDispatcher.assignFeedbackListener(feedbackListener);

    eventDispatcher.onFeedbackOpened();

    verify(feedbackListener, times(0)).onFeedbackCancelled();
  }

  @Test
  public void onFeedbackListenerNotSet_feedbackSentIsNotCalled() throws Exception {
    NavigationViewEventDispatcher eventDispatcher = new NavigationViewEventDispatcher();
    FeedbackListener feedbackListener = mock(FeedbackListener.class);
    FeedbackItem item = mock(FeedbackItem.class);

    eventDispatcher.onFeedbackSent(item);

    verify(feedbackListener, times(0)).onFeedbackSent(item);
  }

  @Test
  public void onInstructionListShown_listenerReturnsTrue() {
    InstructionListListener instructionListListener = mock(InstructionListListener.class);
    NavigationViewEventDispatcher eventDispatcher = buildViewEventDispatcher(instructionListListener);

    eventDispatcher.onInstructionListVisibilityChanged(true);

    verify(instructionListListener, times(1)).onInstructionListVisibilityChanged(true);
  }

  @Test
  public void onInstructionListHidden_listenerReturnsFalse() {
    InstructionListListener instructionListListener = mock(InstructionListListener.class);
    NavigationViewEventDispatcher eventDispatcher = buildViewEventDispatcher(instructionListListener);

    eventDispatcher.onInstructionListVisibilityChanged(false);

    verify(instructionListListener, times(1)).onInstructionListVisibilityChanged(false);
  }

  @Test
  public void onProgressChangeListenerAddedInOptions_isAddedToNavigation() {
    NavigationViewEventDispatcher eventDispatcher = new NavigationViewEventDispatcher();
    NavigationViewOptions options = mock(NavigationViewOptions.class);
    ProgressChangeListener progressChangeListener = setupProgressChangeListener(options);
    MapboxNavigation navigation = mock(MapboxNavigation.class);

    eventDispatcher.initializeListeners(options, navigation);

    verify(navigation, times(1)).addProgressChangeListener(progressChangeListener);
  }

  @Test
  public void onProgressChangeListenerAddedInOptions_isRemovedInOnDestroy() {
    NavigationViewEventDispatcher eventDispatcher = new NavigationViewEventDispatcher();
    NavigationViewOptions options = mock(NavigationViewOptions.class);
    ProgressChangeListener progressChangeListener = setupProgressChangeListener(options);
    MapboxNavigation navigation = mock(MapboxNavigation.class);
    eventDispatcher.initializeListeners(options, navigation);

    eventDispatcher.onDestroy(navigation);

    verify(navigation, times(1)).removeProgressChangeListener(progressChangeListener);
  }

  @Test
  public void onMilestoneEventListenerAddedInOptions_isAddedToNavigation() {
    NavigationViewEventDispatcher eventDispatcher = new NavigationViewEventDispatcher();
    NavigationViewOptions options = mock(NavigationViewOptions.class);
    MilestoneEventListener milestoneEventListener = setupMilestoneEventListener(options);
    MapboxNavigation navigation = mock(MapboxNavigation.class);

    eventDispatcher.initializeListeners(options, navigation);

    verify(navigation, times(1)).addMilestoneEventListener(milestoneEventListener);
  }

  @Test
  public void onMilestoneEventListenerAddedInOptions_isRemovedInOnDestroy() {
    NavigationViewEventDispatcher eventDispatcher = new NavigationViewEventDispatcher();
    NavigationViewOptions options = mock(NavigationViewOptions.class);
    MilestoneEventListener milestoneEventListener = setupMilestoneEventListener(options);
    MapboxNavigation navigation = mock(MapboxNavigation.class);
    eventDispatcher.initializeListeners(options, navigation);

    eventDispatcher.onDestroy(navigation);

    verify(navigation, times(1)).removeMilestoneEventListener(milestoneEventListener);
  }

  @Test
  public void onNewBannerInstruction_instructionListenerIsCalled() {
    BannerInstructions modifiedInstructions = mock(BannerInstructions.class);
    BannerInstructions originalInstructions = mock(BannerInstructions.class);
    BannerInstructionsListener bannerInstructionsListener = mock(BannerInstructionsListener.class);
    when(bannerInstructionsListener.willDisplay(originalInstructions)).thenReturn(modifiedInstructions);
    NavigationViewEventDispatcher eventDispatcher = buildViewEventDispatcher(bannerInstructionsListener);

    eventDispatcher.onBannerDisplay(originalInstructions);

    verify(bannerInstructionsListener).willDisplay(originalInstructions);
  }

  @Test
  public void onNewVoiceAnnouncement_instructionListenerIsCalled() {
    SpeechAnnouncement modifiedAnnouncement = mock(SpeechAnnouncement.class);
    SpeechAnnouncement originalAnnouncement = mock(SpeechAnnouncement.class);
    SpeechAnnouncementListener speechAnnouncementListener = mock(SpeechAnnouncementListener.class);
    when(speechAnnouncementListener.willVoice(originalAnnouncement)).thenReturn(modifiedAnnouncement);
    NavigationViewEventDispatcher eventDispatcher = buildViewEventDispatcher(speechAnnouncementListener);

    eventDispatcher.onAnnouncement(originalAnnouncement);

    verify(speechAnnouncementListener).willVoice(originalAnnouncement);
  }

  @NonNull
  private NavigationViewEventDispatcher buildViewEventDispatcher(SpeechAnnouncementListener speechAnnouncementListener) {
    NavigationViewEventDispatcher eventDispatcher = new NavigationViewEventDispatcher();
    eventDispatcher.assignSpeechAnnouncementListener(speechAnnouncementListener);
    return eventDispatcher;
  }

  @NonNull
  private NavigationViewEventDispatcher buildViewEventDispatcher(BannerInstructionsListener bannerInstructionsListener) {
    NavigationViewEventDispatcher eventDispatcher = new NavigationViewEventDispatcher();
    eventDispatcher.assignBannerInstructionsListener(bannerInstructionsListener);
    return eventDispatcher;
  }


  @NonNull
  private NavigationViewEventDispatcher buildViewEventDispatcher(InstructionListListener instructionListListener) {
    NavigationViewEventDispatcher eventDispatcher = new NavigationViewEventDispatcher();
    eventDispatcher.assignInstructionListListener(instructionListListener);
    return eventDispatcher;
  }

  private ProgressChangeListener setupProgressChangeListener(NavigationViewOptions options) {
    ProgressChangeListener progressChangeListener = mock(ProgressChangeListener.class);
    when(options.progressChangeListener()).thenReturn(progressChangeListener);
    return  progressChangeListener;
  }

  private MilestoneEventListener setupMilestoneEventListener(NavigationViewOptions options) {
    MilestoneEventListener milestoneEventListener = mock(MilestoneEventListener.class);
    when(options.milestoneEventListener()).thenReturn(milestoneEventListener);
    return  milestoneEventListener;
  }
}