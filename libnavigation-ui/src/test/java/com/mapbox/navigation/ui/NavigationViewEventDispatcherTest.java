package com.mapbox.navigation.ui;

import androidx.annotation.NonNull;

import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.api.directions.v5.models.VoiceInstructions;
import com.mapbox.navigation.core.MapboxNavigation;
import com.mapbox.navigation.core.trip.session.RouteProgressObserver;
import com.mapbox.navigation.ui.feedback.FeedbackItem;
import com.mapbox.navigation.ui.listeners.BannerInstructionsListener;
import com.mapbox.navigation.ui.listeners.FeedbackListener;
import com.mapbox.navigation.ui.listeners.InstructionListListener;
import com.mapbox.navigation.ui.listeners.NavigationListener;
import com.mapbox.navigation.ui.listeners.SpeechAnnouncementListener;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NavigationViewEventDispatcherTest {

  @Test
  public void sanity() {
    NavigationViewEventDispatcher eventDispatcher = new NavigationViewEventDispatcher();

    assertNotNull(eventDispatcher);
  }

  @Test
  public void setNavigationListener_cancelListenerIsCalled() {
    NavigationViewEventDispatcher eventDispatcher = new NavigationViewEventDispatcher();
    NavigationListener navigationListener = mock(NavigationListener.class);
    NavigationViewModel viewModel = mock(NavigationViewModel.class);
    eventDispatcher.assignNavigationListener(navigationListener, viewModel);

    eventDispatcher.onCancelNavigation();

    verify(navigationListener, times(1)).onCancelNavigation();
  }

  @Test
  public void setNavigationListener_runningListenerCalledIfRunning() {
    NavigationViewEventDispatcher eventDispatcher = new NavigationViewEventDispatcher();
    NavigationListener navigationListener = mock(NavigationListener.class);
    NavigationViewModel viewModel = mock(NavigationViewModel.class);
    when(viewModel.isRunning()).thenReturn(true);

    eventDispatcher.assignNavigationListener(navigationListener, viewModel);

    verify(navigationListener, times(1)).onNavigationRunning();
  }

  @Test
  public void setNavigationListener_finishedListenerIsCalled() {
    NavigationViewEventDispatcher eventDispatcher = new NavigationViewEventDispatcher();
    NavigationListener navigationListener = mock(NavigationListener.class);
    NavigationViewModel viewModel = mock(NavigationViewModel.class);
    eventDispatcher.assignNavigationListener(navigationListener, viewModel);

    eventDispatcher.onNavigationFinished();

    verify(navigationListener, times(1)).onNavigationFinished();
  }

  @Test
  public void setNavigationListener_runningListenerIsCalled() {
    NavigationViewEventDispatcher eventDispatcher = new NavigationViewEventDispatcher();
    NavigationListener navigationListener = mock(NavigationListener.class);
    NavigationViewModel viewModel = mock(NavigationViewModel.class);
    eventDispatcher.assignNavigationListener(navigationListener, viewModel);

    eventDispatcher.onNavigationRunning();

    verify(navigationListener, times(1)).onNavigationRunning();
  }

  @Test
  public void onNavigationListenerNotSet_runningListenerIsNotCalled() {
    NavigationViewEventDispatcher eventDispatcher = new NavigationViewEventDispatcher();
    NavigationListener navigationListener = mock(NavigationListener.class);

    eventDispatcher.onNavigationRunning();

    verify(navigationListener, times(0)).onNavigationRunning();
  }

  @Test
  public void onNavigationListenerNotSet_cancelListenerIsNotCalled() {
    NavigationViewEventDispatcher eventDispatcher = new NavigationViewEventDispatcher();
    NavigationListener navigationListener = mock(NavigationListener.class);

    eventDispatcher.onCancelNavigation();

    verify(navigationListener, times(0)).onCancelNavigation();
  }

  @Test
  public void onNavigationListenerNotSet_finishedListenerIsNotCalled() {
    NavigationViewEventDispatcher eventDispatcher = new NavigationViewEventDispatcher();
    NavigationListener navigationListener = mock(NavigationListener.class);

    eventDispatcher.onNavigationFinished();

    verify(navigationListener, times(0)).onNavigationFinished();
  }

  @Test
  public void setFeedbackListener_feedbackOpenIsCalled() {
    NavigationViewEventDispatcher eventDispatcher = new NavigationViewEventDispatcher();
    FeedbackListener feedbackListener = mock(FeedbackListener.class);
    eventDispatcher.assignFeedbackListener(feedbackListener);

    eventDispatcher.onFeedbackOpened();

    verify(feedbackListener, times(1)).onFeedbackOpened();
  }

  @Test
  public void setFeedbackListener_feedbackCancelledIsCalled() {
    NavigationViewEventDispatcher eventDispatcher = new NavigationViewEventDispatcher();
    FeedbackListener feedbackListener = mock(FeedbackListener.class);
    eventDispatcher.assignFeedbackListener(feedbackListener);

    eventDispatcher.onFeedbackCancelled();

    verify(feedbackListener, times(1)).onFeedbackCancelled();
  }

  @Test
  public void setFeedbackListener_feedbackSentIsCalled() {
    NavigationViewEventDispatcher eventDispatcher = new NavigationViewEventDispatcher();
    FeedbackListener feedbackListener = mock(FeedbackListener.class);
    eventDispatcher.assignFeedbackListener(feedbackListener);
    FeedbackItem item = mock(FeedbackItem.class);
    eventDispatcher.assignFeedbackListener(feedbackListener);

    eventDispatcher.onFeedbackSent(item);

    verify(feedbackListener, times(1)).onFeedbackSent(item);
  }

  @Test
  public void onFeedbackListenerNotSet_feedbackOpenedIsNotCalled() {
    NavigationViewEventDispatcher eventDispatcher = new NavigationViewEventDispatcher();
    FeedbackListener feedbackListener = mock(FeedbackListener.class);

    eventDispatcher.onFeedbackOpened();

    verify(feedbackListener, times(0)).onFeedbackOpened();
  }

  @Test
  public void onFeedbackListenerNotSet_feedbackCancelledIsNotCalled() {
    NavigationViewEventDispatcher eventDispatcher = new NavigationViewEventDispatcher();
    FeedbackListener feedbackListener = mock(FeedbackListener.class);
    eventDispatcher.assignFeedbackListener(feedbackListener);

    eventDispatcher.onFeedbackOpened();

    verify(feedbackListener, times(0)).onFeedbackCancelled();
  }

  @Test
  public void onFeedbackListenerNotSet_feedbackSentIsNotCalled() {
    NavigationViewEventDispatcher eventDispatcher = new NavigationViewEventDispatcher();
    FeedbackListener feedbackListener = mock(FeedbackListener.class);
    FeedbackItem item = mock(FeedbackItem.class);

    eventDispatcher.onFeedbackSent(item);

    verify(feedbackListener, times(0)).onFeedbackSent(item);
  }

  @Test
  public void onInstructionListShown_listenerReturnsTrue() {
    InstructionListListener instructionListListener = mock(InstructionListListener.class);
    NavigationViewEventDispatcher eventDispatcher = buildDispatcher(instructionListListener);

    eventDispatcher.onInstructionListVisibilityChanged(true);

    verify(instructionListListener, times(1)).onInstructionListVisibilityChanged(true);
  }

  @Test
  public void onInstructionListHidden_listenerReturnsFalse() {
    InstructionListListener instructionListListener = mock(InstructionListListener.class);
    NavigationViewEventDispatcher eventDispatcher = buildDispatcher(instructionListListener);

    eventDispatcher.onInstructionListVisibilityChanged(false);

    verify(instructionListListener, times(1)).onInstructionListVisibilityChanged(false);
  }

  @Test
  public void onRouteProgressObserverAddedInOptions_isAddedToNavigation() {
    NavigationViewEventDispatcher eventDispatcher = new NavigationViewEventDispatcher();
    NavigationViewOptions options = mock(NavigationViewOptions.class);
    RouteProgressObserver routeProgressObserver = setupRouteProgressObserver(options);
    NavigationViewModel navigationViewModel = mock(NavigationViewModel.class);
    MapboxNavigation navigation = mock(MapboxNavigation.class);
    when(navigationViewModel.retrieveNavigation()).thenReturn(navigation);

    eventDispatcher.initializeListeners(options, navigationViewModel);

    verify(navigation, times(1)).registerRouteProgressObserver(routeProgressObserver);
  }

  @Test
  public void onRouteProgressObserverAddedInOptions_isRemovedInOnDestroy() {
    NavigationViewEventDispatcher eventDispatcher = new NavigationViewEventDispatcher();
    NavigationViewOptions options = mock(NavigationViewOptions.class);
    NavigationViewModel navigationViewModel = mock(NavigationViewModel.class);
    MapboxNavigation navigation = mock(MapboxNavigation.class);
    when(navigationViewModel.retrieveNavigation()).thenReturn(navigation);
    RouteProgressObserver routeProgressObserver = setupRouteProgressObserver(options);
    eventDispatcher.initializeListeners(options, navigationViewModel);

    eventDispatcher.onDestroy();

    verify(navigation, times(1)).unregisterRouteProgressObserver(routeProgressObserver);
  }

  @Test
  public void onNewBannerInstruction_instructionListenerIsCalled() {
    BannerInstructions modifiedInstructions = mock(BannerInstructions.class);
    BannerInstructions originalInstructions = mock(BannerInstructions.class);
    BannerInstructionsListener bannerInstructionsListener = mock(BannerInstructionsListener.class);
    when(bannerInstructionsListener.willDisplay(originalInstructions)).thenReturn(modifiedInstructions);
    NavigationViewEventDispatcher eventDispatcher = buildDispatcher(bannerInstructionsListener);

    eventDispatcher.onBannerDisplay(originalInstructions);

    verify(bannerInstructionsListener).willDisplay(originalInstructions);
  }

  @Test
  public void onNewVoiceAnnouncement_instructionListenerIsCalled() {
    VoiceInstructions originalAnnouncement = mock(VoiceInstructions.class);
    SpeechAnnouncementListener speechAnnouncementListener = mock(SpeechAnnouncementListener.class);
    VoiceInstructions newAnnouncement = VoiceInstructions.builder()
      .announcement("New announcement").build();
    when(speechAnnouncementListener.willVoice(originalAnnouncement)).thenReturn(newAnnouncement);
    NavigationViewEventDispatcher eventDispatcher = buildDispatcher(speechAnnouncementListener);

    eventDispatcher.onAnnouncement(originalAnnouncement);

    verify(speechAnnouncementListener).willVoice(originalAnnouncement);
  }

  @Test
  public void onNewVoiceAnnouncement_announcementToBeVoicedIsReturned() {
    VoiceInstructions originalAnnouncement = VoiceInstructions.builder().announcement("announcement").build();
    SpeechAnnouncementListener speechAnnouncementListener = mock(SpeechAnnouncementListener.class);
    VoiceInstructions newAnnouncement = VoiceInstructions.builder()
      .announcement("New announcement").build();
    when(speechAnnouncementListener.willVoice(originalAnnouncement)).thenReturn(newAnnouncement);
    NavigationViewEventDispatcher eventDispatcher = buildDispatcher(speechAnnouncementListener);

    VoiceInstructions modifiedAnnouncement = eventDispatcher.onAnnouncement(originalAnnouncement);

    assertEquals("New announcement", modifiedAnnouncement.announcement());
  }

  @Test
  public void onNewVoiceAnnouncement_ssmlAnnouncementToBeVoicedIsReturned() {
    VoiceInstructions originalAnnouncement = VoiceInstructions.builder().announcement("announcement").build();
    SpeechAnnouncementListener speechAnnouncementListener = mock(SpeechAnnouncementListener.class);
    VoiceInstructions newAnnouncement = VoiceInstructions.builder()
      .announcement("New announcement")
      .ssmlAnnouncement("New SSML announcement").build();
    when(speechAnnouncementListener.willVoice(originalAnnouncement)).thenReturn(newAnnouncement);
    NavigationViewEventDispatcher eventDispatcher = buildDispatcher(speechAnnouncementListener);

    VoiceInstructions modifiedAnnouncement = eventDispatcher.onAnnouncement(originalAnnouncement);

    assertEquals("New SSML announcement", modifiedAnnouncement.ssmlAnnouncement());
  }

  @NonNull
  private NavigationViewEventDispatcher buildDispatcher(SpeechAnnouncementListener speechAnnouncementListener) {
    NavigationViewEventDispatcher eventDispatcher = new NavigationViewEventDispatcher();
    eventDispatcher.assignSpeechAnnouncementListener(speechAnnouncementListener);
    return eventDispatcher;
  }

  @NonNull
  private NavigationViewEventDispatcher buildDispatcher(BannerInstructionsListener bannerInstructionsListener) {
    NavigationViewEventDispatcher eventDispatcher = new NavigationViewEventDispatcher();
    eventDispatcher.assignBannerInstructionsListener(bannerInstructionsListener);
    return eventDispatcher;
  }


  @NonNull
  private NavigationViewEventDispatcher buildDispatcher(InstructionListListener instructionListListener) {
    NavigationViewEventDispatcher eventDispatcher = new NavigationViewEventDispatcher();
    eventDispatcher.assignInstructionListListener(instructionListListener);
    return eventDispatcher;
  }

  private RouteProgressObserver setupRouteProgressObserver(NavigationViewOptions options) {
    RouteProgressObserver routeProgressObserver = mock(RouteProgressObserver.class);
    when(options.routeProgressObserver()).thenReturn(routeProgressObserver);
    return routeProgressObserver;
  }
}