package com.mapbox.services.android.navigation.ui.v5;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NavigationPresenterTest {

  @Test
  public void onRouteOverviewButtonClick_cameraIsAdjustedToRoute() {
    NavigationContract.View view = mock(NavigationContract.View.class);
    NavigationPresenter presenter = new NavigationPresenter(view);

    presenter.onRouteOverviewClick();

    verify(view).updateCameraRouteOverview();
  }

  @Test
  public void onRouteOverviewButtonClick_recenterBtnIsShown() {
    NavigationContract.View view = mock(NavigationContract.View.class);
    NavigationPresenter presenter = new NavigationPresenter(view);

    presenter.onRouteOverviewClick();

    verify(view).showRecenterBtn();
  }

  @Test
  public void onRouteOverviewButtonClick_mapWaynameIsHidden() {
    NavigationContract.View view = mock(NavigationContract.View.class);
    NavigationPresenter presenter = new NavigationPresenter(view);

    presenter.onRouteOverviewClick();

    verify(view).updateWayNameVisibility(false);
  }

  @Test
  public void onRecenterBtnClick_recenterBtnIsHidden() {
    NavigationContract.View view = mock(NavigationContract.View.class);
    NavigationPresenter presenter = new NavigationPresenter(view);

    presenter.onRecenterClick();

    verify(view).hideRecenterBtn();
  }

  @Test
  public void onRecenterBtnClick_cameraIsResetToTracking() {
    NavigationContract.View view = mock(NavigationContract.View.class);
    NavigationPresenter presenter = new NavigationPresenter(view);

    presenter.onRecenterClick();

    verify(view).resetCameraPosition();
  }

  @Test
  public void onRecenterBtnClick_mapWayNameIsShown() {
    NavigationContract.View view = mock(NavigationContract.View.class);
    NavigationPresenter presenter = new NavigationPresenter(view);

    presenter.onRecenterClick();

    verify(view).updateWayNameVisibility(true);
  }

  @Test
  public void onWayNameChanged_mapWayNameIsShown() {
    NavigationContract.View view = mock(NavigationContract.View.class);
    NavigationPresenter presenter = new NavigationPresenter(view);

    presenter.onWayNameChanged("Some way name");

    verify(view).updateWayNameVisibility(true);
  }

  @Test
  public void onWayNameChanged_mapWayNameIsUpdated() {
    String someWayName = "Some way name";
    NavigationContract.View view = mock(NavigationContract.View.class);
    NavigationPresenter presenter = new NavigationPresenter(view);

    presenter.onWayNameChanged(someWayName);

    verify(view).updateWayNameView(someWayName);
  }

  @Test
  public void onWayNameChanged_mapWayNameIsHidden() {
    NavigationContract.View view = mock(NavigationContract.View.class);
    NavigationPresenter presenter = new NavigationPresenter(view);

    presenter.onWayNameChanged("");

    verify(view).updateWayNameVisibility(false);
  }

  @Test
  public void onWayNameChanged_mapWayNameIsHiddenWithCollapsedBottomsheet() {
    NavigationContract.View view = mock(NavigationContract.View.class);
    when(view.isSummaryBottomSheetHidden()).thenReturn(true);
    NavigationPresenter presenter = new NavigationPresenter(view);

    presenter.onWayNameChanged("some valid way name");

    verify(view).updateWayNameVisibility(false);
  }

  @Test
  public void onNavigationStopped_mapWayNameIsHidden() {
    NavigationContract.View view = mock(NavigationContract.View.class);
    NavigationPresenter presenter = new NavigationPresenter(view);

    presenter.onNavigationStopped();

    verify(view).updateWayNameVisibility(false);
  }
}
