package com.mapbox.navigation.ui.internal.summary;

import android.text.SpannableString;

import androidx.annotation.NonNull;

import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.navigation.base.formatter.DistanceFormatter;
import com.mapbox.navigation.base.trip.model.RouteProgress;
import com.mapbox.navigation.ui.BaseTest;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

public class InstructionListPresenterTest extends BaseTest {

  @Test
  public void onBindInstructionListView_distanceTextIsUpdated() throws Exception {
    SpannableString spannableString = mock(SpannableString.class);
    InstructionListAdapter adapter = mock(InstructionListAdapter.class);
    RouteProgress routeProgress = buildRouteProgress();
    InstructionListPresenter presenter = buildPresenter(spannableString);
    presenter.update(routeProgress, adapter);
    InstructionListView listView = mock(InstructionListView.class);

    presenter.onBindInstructionListViewAtPosition(0, listView);

    verify(listView).updateDistanceText(spannableString);
  }

  @Test
  public void onBindInstructionListView_primaryTextIsUpdated() throws Exception {
    SpannableString spannableString = mock(SpannableString.class);
    InstructionListAdapter adapter = mock(InstructionListAdapter.class);
    RouteProgress routeProgress = buildRouteProgress();
    InstructionListPresenter presenter = buildPresenter(spannableString);
    presenter.update(routeProgress, adapter);
    InstructionListView listView = mock(InstructionListView.class);

    presenter.onBindInstructionListViewAtPosition(0, listView);

    verify(listView).updatePrimaryText(anyString());
  }

  @Test
  public void onBindInstructionListView_secondaryTextIsUpdated() throws Exception {
    SpannableString spannableString = mock(SpannableString.class);
    InstructionListAdapter adapter = mock(InstructionListAdapter.class);
    RouteProgress routeProgress = buildRouteProgress();
    InstructionListPresenter presenter = buildPresenter(spannableString);
    presenter.update(routeProgress, adapter);
    InstructionListView listView = mock(InstructionListView.class);

    presenter.onBindInstructionListViewAtPosition(0, listView);

    verify(listView).updateSecondaryText(anyString());
  }

  @Test
  public void onBindInstructionListView_maneuverViewTypeAndModifierAreUpdated() throws Exception {
    SpannableString spannableString = mock(SpannableString.class);
    InstructionListAdapter adapter = mock(InstructionListAdapter.class);
    RouteProgress routeProgress = buildRouteProgress();
    InstructionListPresenter presenter = buildPresenter(spannableString);
    presenter.update(routeProgress, adapter);
    InstructionListView listView = mock(InstructionListView.class);

    presenter.onBindInstructionListViewAtPosition(0, listView);

    verify(listView).updateManeuverViewTypeAndModifier(anyString(), anyString());
  }

  @Test
  public void onBindInstructionListView_maneuverViewDrivingSideIsUpdated() throws Exception {
    SpannableString spannableString = mock(SpannableString.class);
    InstructionListAdapter adapter = mock(InstructionListAdapter.class);
    RouteProgress routeProgress = buildRouteProgress();
    InstructionListPresenter presenter = buildPresenter(spannableString);
    presenter.update(routeProgress, adapter);
    InstructionListView listView = mock(InstructionListView.class);

    presenter.onBindInstructionListViewAtPosition(0, listView);

    verify(listView).updateManeuverViewDrivingSide(anyString());
  }

  @Test
  public void update_excludesCurrentStepFromInstructionList() throws Exception {
    RouteProgress routeProgress = buildRouteProgressWithProgress();
    InstructionListAdapter adapter = mock(InstructionListAdapter.class);
    DistanceFormatter distanceFormatter = mock(DistanceFormatter.class);
    InstructionListPresenter presenter = new InstructionListPresenter(distanceFormatter);

    presenter.update(routeProgress, adapter);

    assertEquals(9, presenter.retrieveBannerInstructionListSize());
  }

  @Test
  public void update_notifyItemRangeInserted() throws Exception {
    RouteProgress routeProgress = buildRouteProgress();
    InstructionListAdapter adapter = mock(InstructionListAdapter.class);
    DistanceFormatter distanceFormatter = mock(DistanceFormatter.class);
    InstructionListPresenter presenter = new InstructionListPresenter(distanceFormatter);

    presenter.update(routeProgress, adapter);

    verify(adapter).notifyItemRangeInserted(0, 12);
  }

  @Test
  public void update_notifyItemRangeRemoved() throws Exception {
    RouteProgress initialRouteProgress = buildRouteProgress();
    RouteProgress updatedRouteProgress = buildRouteProgressWithProgress();
    InstructionListAdapter adapter = mock(InstructionListAdapter.class);
    DistanceFormatter distanceFormatter = mock(DistanceFormatter.class);
    InstructionListPresenter presenter = new InstructionListPresenter(distanceFormatter);

    presenter.update(initialRouteProgress, adapter);
    presenter.update(updatedRouteProgress, adapter);

    verify(adapter).notifyItemRangeRemoved(0, 3);
  }

  @Test
  public void updateDistanceFormatter_newFormatterIsUsed() throws Exception {
    RouteProgress routeProgress = buildRouteProgress();
    InstructionListAdapter adapter = mock(InstructionListAdapter.class);
    DistanceFormatter firstDistanceFormatter = buildDistanceFormatter();
    InstructionListPresenter presenter = buildPresenter(firstDistanceFormatter);
    presenter.update(routeProgress, adapter);
    InstructionListView listView = mock(InstructionListView.class);

    DistanceFormatter secondDistanceFormatter = buildDistanceFormatter();
    presenter.updateDistanceFormatter(secondDistanceFormatter);
    presenter.onBindInstructionListViewAtPosition(0, listView);

    verify(secondDistanceFormatter).formatDistance(anyDouble());
  }

  @Test
  public void updateDistanceFormatter_doesNotAllowNullValues() throws Exception {
    RouteProgress routeProgress = buildRouteProgress();
    InstructionListAdapter adapter = mock(InstructionListAdapter.class);
    DistanceFormatter distanceFormatter = buildDistanceFormatter();
    InstructionListPresenter presenter = buildPresenter(distanceFormatter);
    presenter.update(routeProgress, adapter);
    InstructionListView listView = mock(InstructionListView.class);

    presenter.updateDistanceFormatter(null);
    presenter.onBindInstructionListViewAtPosition(0, listView);

    verify(distanceFormatter).formatDistance(anyDouble());
  }

  @Test
  public void findCurrentBannerInstructions_returnsNullWithNullCurrentStep() throws Exception {
    LegStep currentStep = null;
    double stepDistanceRemaining = 0;
    SpannableString spannableString = mock(SpannableString.class);
    InstructionListPresenter presenter = buildPresenter(spannableString);
    BannerInstructions currentBannerInstructions = presenter.findCurrentBannerInstructions(
      currentStep,
      stepDistanceRemaining
    );
    assertNull(currentBannerInstructions);
  }

  @Test
  public void findCurrentBannerInstructions_returnsNullWithCurrentStepEmptyInstructions() throws Exception {
    SpannableString spannableString = mock(SpannableString.class);
    RouteProgress routeProgress = buildRouteProgress();
    InstructionListPresenter presenter = buildPresenter(spannableString);
    LegStep currentStep = routeProgress.getCurrentLegProgress().getCurrentStepProgress().getStep();
    double stepDistanceRemaining = routeProgress.getCurrentLegProgress().getCurrentStepProgress().getDistanceRemaining();
    List<BannerInstructions> currentInstructions = currentStep.bannerInstructions();
    currentInstructions.clear();
    BannerInstructions currentBannerInstructions = presenter.findCurrentBannerInstructions(
      currentStep,
      stepDistanceRemaining
    );
    assertNull(currentBannerInstructions);
  }

  @Test
  public void retrieveBannerInstructionListSize_returnsCorrectListSize() throws Exception {
    InstructionListAdapter adapter = mock(InstructionListAdapter.class);
    RouteProgress routeProgress = buildRouteProgress();
    DistanceFormatter distanceFormatter = mock(DistanceFormatter.class);
    InstructionListPresenter presenter = new InstructionListPresenter(distanceFormatter);

    presenter.update(routeProgress, adapter);

    int expectedInstructionSize = retrieveInstructionSizeFrom(routeProgress.getCurrentLegProgress().getRouteLeg());
    assertEquals(expectedInstructionSize, presenter.retrieveBannerInstructionListSize());
  }

  @Test
  public void findCurrentBannerInstructions_returnsCorrectCurrentInstruction() throws Exception {
    SpannableString spannableString = mock(SpannableString.class);
    RouteProgress routeProgress = buildRouteProgress();
    InstructionListPresenter presenter = buildPresenter(spannableString);
    LegStep currentStep = routeProgress.getCurrentLegProgress().getCurrentStepProgress().getStep();
    double stepDistanceRemaining = routeProgress.getCurrentLegProgress().getCurrentStepProgress().getDistanceRemaining();
    BannerInstructions currentBannerInstructions = presenter.findCurrentBannerInstructions(
      currentStep,
      stepDistanceRemaining
    );
    assertEquals(currentStep.bannerInstructions().get(0), currentBannerInstructions);
  }

  @Test
  public void findCurrentBannerInstructions_adjustedDistanceRemainingReturnsCorrectInstruction() throws Exception {
    SpannableString spannableString = mock(SpannableString.class);
    RouteProgress routeProgress = buildRouteProgress();
    InstructionListPresenter presenter = buildPresenter(spannableString);
    LegStep currentStep = routeProgress.getCurrentLegProgress().getCurrentStepProgress().getStep();
    double stepDistanceRemaining = routeProgress.getCurrentLegProgress().getCurrentStepProgress().getDistanceRemaining();
    BannerInstructions currentBannerInstructions = presenter.findCurrentBannerInstructions(
      currentStep,
      stepDistanceRemaining
    );
    assertEquals(currentStep.bannerInstructions().get(0), currentBannerInstructions);
  }

  @Test
  public void findCurrentBannerInstructions_adjustedDistanceRemainingRemovesCorrectInstructions() throws Exception {
    SpannableString spannableString = mock(SpannableString.class);
    RouteProgress routeProgress = buildRouteProgress();
    InstructionListPresenter presenter = buildPresenter(spannableString);
    LegStep currentStep = routeProgress.getCurrentLegProgress().getCurrentStepProgress().getStep();
    double stepDistanceRemaining = routeProgress.getCurrentLegProgress().getCurrentStepProgress().getDistanceRemaining();
    BannerInstructions currentBannerInstructions = presenter.findCurrentBannerInstructions(
      currentStep,
      stepDistanceRemaining
    );
    assertEquals(currentStep.bannerInstructions().get(0), currentBannerInstructions);
  }


  @NonNull
  private RouteProgress buildRouteProgress() throws Exception {
    DirectionsRoute route = buildTestDirectionsRoute();
    return buildRouteProgress(route, 100, 100, 100, 0, 0);
  }

  @NonNull
  private RouteProgress buildRouteProgressWithProgress() throws Exception {
    DirectionsRoute route = buildTestDirectionsRoute();
    return buildRouteProgress(route, 100, 100, 100, 2, 0);
  }

  @NonNull
  private InstructionListPresenter buildPresenter(SpannableString spannableString) {
    DistanceFormatter distanceFormatter = mock(DistanceFormatter.class);
    when(distanceFormatter.formatDistance(anyDouble())).thenReturn(spannableString);
    return new InstructionListPresenter(distanceFormatter);
  }

  @NonNull
  private InstructionListPresenter buildPresenter(DistanceFormatter distanceFormatter) {
    return new InstructionListPresenter(distanceFormatter);
  }

  @NonNull
  private DistanceFormatter buildDistanceFormatter() {
    SpannableString spannableString = mock(SpannableString.class);
    DistanceFormatter distanceFormatter = mock(DistanceFormatter.class);
    when(distanceFormatter.formatDistance(anyDouble())).thenReturn(spannableString);
    return distanceFormatter;
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
    for (LegStep step : routeProgress.getCurrentLegProgress().getRouteLeg().steps()) {
      List<BannerInstructions> instructions = step.bannerInstructions();
      if (instructions != null) {
        instructions.clear();
      }
    }
  }
}