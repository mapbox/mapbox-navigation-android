package com.mapbox.services.android.navigation.ui.v5.summary.list;

import android.support.annotation.NonNull;
import android.text.SpannableString;

import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.services.android.navigation.ui.v5.BaseTest;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.DistanceFormatter;
import com.mapbox.services.android.navigation.v5.utils.RouteUtils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class InstructionListPresenterTest extends BaseTest {

  private static final int FIRST = 0;

  @Test
  public void onBindInstructionListView_distanceTextIsUpdated() throws Exception {
    SpannableString spannableString = mock(SpannableString.class);
    RouteProgress routeProgress = buildRouteProgress();
    InstructionListPresenter presenter = buildPresenter(spannableString, routeProgress);
    presenter.updateBannerListWith(routeProgress);
    InstructionListView listView = mock(InstructionListView.class);

    presenter.onBindInstructionListViewAtPosition(0, listView);

    verify(listView).updateDistanceText(spannableString);
  }

  @Test
  public void onBindInstructionListView_primaryTextIsUpdated() throws Exception {
    SpannableString spannableString = mock(SpannableString.class);
    RouteProgress routeProgress = buildRouteProgress();
    InstructionListPresenter presenter = buildPresenter(spannableString, routeProgress);
    presenter.updateBannerListWith(routeProgress);
    InstructionListView listView = mock(InstructionListView.class);

    presenter.onBindInstructionListViewAtPosition(0, listView);

    verify(listView).updatePrimaryText(anyString());
  }

  @Test
  public void onBindInstructionListView_secondaryTextIsUpdated() throws Exception {
    SpannableString spannableString = mock(SpannableString.class);
    RouteProgress routeProgress = buildRouteProgress();
    InstructionListPresenter presenter = buildPresenter(spannableString, routeProgress);
    presenter.updateBannerListWith(routeProgress);
    InstructionListView listView = mock(InstructionListView.class);

    presenter.onBindInstructionListViewAtPosition(0, listView);

    verify(listView).updateSecondaryText(anyString());
  }

  @Test
  public void onBindInstructionListView_maneuverViewIsUpdated() throws Exception {
    SpannableString spannableString = mock(SpannableString.class);
    RouteProgress routeProgress = buildRouteProgress();
    InstructionListPresenter presenter = buildPresenter(spannableString, routeProgress);
    presenter.updateBannerListWith(routeProgress);
    InstructionListView listView = mock(InstructionListView.class);

    presenter.onBindInstructionListViewAtPosition(0, listView);

    verify(listView).updateManeuverViewTypeAndModifier(anyString(), anyString());
  }

  @Test
  public void retrieveBannerInstructionListSize_returnsCorrectListSize() throws Exception {
    RouteProgress routeProgress = buildRouteProgress();
    RouteUtils routeUtils = buildRouteUtils(routeProgress);
    DistanceFormatter distanceFormatter = mock(DistanceFormatter.class);
    InstructionListPresenter presenter = new InstructionListPresenter(routeUtils, distanceFormatter);

    presenter.updateBannerListWith(routeProgress);

    int expectedInstructionSize = retrieveInstructionSizeFrom(routeProgress.currentLeg());
    assertEquals(expectedInstructionSize, presenter.retrieveBannerInstructionListSize());
  }

  @Test
  public void updateBannerListWith_instructionListIsPopulated() throws Exception {
    RouteProgress routeProgress = buildRouteProgress();
    RouteUtils routeUtils = buildRouteUtils(routeProgress);
    DistanceFormatter distanceFormatter = mock(DistanceFormatter.class);
    InstructionListPresenter presenter = new InstructionListPresenter(routeUtils, distanceFormatter);

    boolean didUpdate = presenter.updateBannerListWith(routeProgress);

    assertTrue(didUpdate);
  }

  @Test
  public void updateBannerListWith_emptyInstructionsReturnFalse() throws Exception {
    RouteProgress routeProgress = buildRouteProgress();
    RouteUtils routeUtils = buildRouteUtils(routeProgress);
    clearInstructions(routeProgress);
    DistanceFormatter distanceFormatter = mock(DistanceFormatter.class);
    InstructionListPresenter presenter = new InstructionListPresenter(routeUtils, distanceFormatter);

    boolean didUpdate = presenter.updateBannerListWith(routeProgress);

    assertFalse(didUpdate);
  }

  @NonNull
  private RouteProgress buildRouteProgress() throws Exception {
    DirectionsRoute route = buildTestDirectionsRoute();
    return buildRouteProgress(route, 100, 100, 100, 0, 0);
  }

  @NonNull
  private InstructionListPresenter buildPresenter(SpannableString spannableString, RouteProgress routeProgress) {
    RouteUtils routeUtils = buildRouteUtils(routeProgress);
    DistanceFormatter distanceFormatter = mock(DistanceFormatter.class);
    when(distanceFormatter.formatDistance(anyDouble())).thenReturn(spannableString);
    return new InstructionListPresenter(routeUtils, distanceFormatter);
  }

  @NonNull
  private RouteUtils buildRouteUtils(RouteProgress routeProgress) {
    RouteUtils routeUtils = mock(RouteUtils.class);
    BannerInstructions instructions = routeProgress.currentLegProgress().currentStep().bannerInstructions().get(FIRST);
    when(routeUtils.findCurrentBannerInstructions(any(LegStep.class), anyDouble())).thenReturn(instructions);
    return routeUtils;
  }

  private int retrieveInstructionSizeFrom(RouteLeg routeLeg) {
    List<BannerInstructions> instructions = new ArrayList<>();
    List<LegStep> steps = routeLeg.steps();
    for (LegStep step : steps) {
      List<BannerInstructions> bannerInstructions = step.bannerInstructions();
      if (bannerInstructions != null) {
        instructions.addAll(bannerInstructions);
      }
    }
    return instructions.size() - 1;
  }

  private void clearInstructions(RouteProgress routeProgress) {
    for (LegStep step : routeProgress.currentLeg().steps()) {
      List<BannerInstructions> instructions = step.bannerInstructions();
      if (instructions != null) {
        instructions.clear();
      }
    }
  }
}