package com.mapbox.services.android.navigation.ui.v5;

import android.arch.lifecycle.LifecycleOwner;

import org.junit.Test;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class NavigationViewSubscriberTest {

  @Test
  public void checksRouteObserversAreRemovedWhenUnsubscribe() {
    NavigationPresenter mockedNavigationPresenter = mock(NavigationPresenter.class);
    NavigationViewSubscriber theNavigationViewSubscriber = new NavigationViewSubscriber(mockedNavigationPresenter);
    LifecycleOwner mockedLifecycleOwner = mock(LifecycleOwner.class);
    NavigationViewModel mockedNavigationViewModel = mock(NavigationViewModel.class, RETURNS_DEEP_STUBS);

    theNavigationViewSubscriber.unsubscribe(mockedLifecycleOwner, mockedNavigationViewModel);

    verify(mockedNavigationViewModel.retrieveRoute()).removeObservers(eq(mockedLifecycleOwner));
  }

  @Test
  public void checksDestinationObserversAreRemovedWhenUnsubscribe() {
    NavigationPresenter mockedNavigationPresenter = mock(NavigationPresenter.class);
    NavigationViewSubscriber theNavigationViewSubscriber = new NavigationViewSubscriber(mockedNavigationPresenter);
    LifecycleOwner mockedLifecycleOwner = mock(LifecycleOwner.class);
    NavigationViewModel mockedNavigationViewModel = mock(NavigationViewModel.class, RETURNS_DEEP_STUBS);

    theNavigationViewSubscriber.unsubscribe(mockedLifecycleOwner, mockedNavigationViewModel);

    verify(mockedNavigationViewModel.retrieveDestination()).removeObservers(eq(mockedLifecycleOwner));
  }

  @Test
  public void checksNavigationLocationObserversAreRemovedWhenUnsubscribe() {
    NavigationPresenter mockedNavigationPresenter = mock(NavigationPresenter.class);
    NavigationViewSubscriber theNavigationViewSubscriber = new NavigationViewSubscriber(mockedNavigationPresenter);
    LifecycleOwner mockedLifecycleOwner = mock(LifecycleOwner.class);
    NavigationViewModel mockedNavigationViewModel = mock(NavigationViewModel.class, RETURNS_DEEP_STUBS);

    theNavigationViewSubscriber.unsubscribe(mockedLifecycleOwner, mockedNavigationViewModel);

    verify(mockedNavigationViewModel.retrieveNavigationLocation()).removeObservers(eq(mockedLifecycleOwner));
  }

  @Test
  public void checksShouldRecordScreenshotObserversAreRemovedWhenUnsubscribe() {
    NavigationPresenter mockedNavigationPresenter = mock(NavigationPresenter.class);
    NavigationViewSubscriber theNavigationViewSubscriber = new NavigationViewSubscriber(mockedNavigationPresenter);
    LifecycleOwner mockedLifecycleOwner = mock(LifecycleOwner.class);
    NavigationViewModel mockedNavigationViewModel = mock(NavigationViewModel.class, RETURNS_DEEP_STUBS);

    theNavigationViewSubscriber.unsubscribe(mockedLifecycleOwner, mockedNavigationViewModel);

    verify(mockedNavigationViewModel.retrieveShouldRecordScreenshot()).removeObservers(eq(mockedLifecycleOwner));
  }
}