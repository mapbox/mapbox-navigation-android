package com.mapbox.services.android.navigation.v5.navigation;

import android.content.Context;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.services.android.navigation.v5.milestone.Milestone;
import com.mapbox.services.android.navigation.v5.milestone.StepMilestone;
import com.mapbox.services.android.navigation.v5.navigation.camera.SimpleCamera;
import com.mapbox.services.android.navigation.v5.offroute.OffRoute;
import com.mapbox.services.android.navigation.v5.offroute.OffRouteDetector;
import com.mapbox.services.android.navigation.v5.snap.Snap;
import com.mapbox.services.android.navigation.v5.snap.SnapToRoute;

import org.junit.Test;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.BANNER_INSTRUCTION_MILESTONE_ID;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.VOICE_INSTRUCTION_MILESTONE_ID;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class MapboxNavigationTest extends BaseTest {

  @Test
  public void sanityTest() {
    MapboxNavigation navigation = buildMapboxNavigation();

    assertNotNull(navigation);
  }

  @Test
  public void sanityTestWithOptions() {
    MapboxNavigationOptions options = MapboxNavigationOptions.builder().build();
    MapboxNavigation navigationWithOptions = buildMapboxNavigationWithOptions(options);

    assertNotNull(navigationWithOptions);
  }

  @Test
  public void voiceMilestone_onInitializationDoesGetAdded() throws Exception {
    MapboxNavigation navigation = buildMapboxNavigation();

    int identifier = navigation.getMilestones().get(0).getIdentifier();

    assertEquals(identifier, VOICE_INSTRUCTION_MILESTONE_ID);
  }

  @Test
  public void bannerMilestone_onInitializationDoesGetAdded() throws Exception {
    MapboxNavigation navigation = buildMapboxNavigation();

    int identifier = navigation.getMilestones().get(1).getIdentifier();

    assertEquals(identifier, BANNER_INSTRUCTION_MILESTONE_ID);
  }


  @Test
  public void defaultMilestones_onInitializationDoNotGetAdded() throws Exception {
    MapboxNavigationOptions options = MapboxNavigationOptions.builder().defaultMilestonesEnabled(false).build();
    MapboxNavigation navigationWithOptions = buildMapboxNavigationWithOptions(options);

    assertEquals(0, navigationWithOptions.getMilestones().size());
  }

  @Test
  public void defaultEngines_offRouteEngineDidGetInitialized() throws Exception {
    MapboxNavigation navigation = buildMapboxNavigation();

    assertNotNull(navigation.getOffRouteEngine());
  }

  @Test
  public void defaultEngines_snapEngineDidGetInitialized() throws Exception {
    MapboxNavigation navigation = buildMapboxNavigation();

    assertNotNull(navigation.getSnapEngine());
  }

  @Test
  public void offRouteEngine_doesNotGetInitializedWithFalseOption() throws Exception {
    MapboxNavigationOptions options = MapboxNavigationOptions.builder()
      .enableOffRouteDetection(false)
      .build();
    MapboxNavigation navigationWithOptions = buildMapboxNavigationWithOptions(options);

    assertNull(navigationWithOptions.getOffRouteEngine());
  }

  @Test
  public void snapToRouteEngine_doesNotGetInitializedWithFalseOption() throws Exception {
    MapboxNavigationOptions options = MapboxNavigationOptions.builder()
      .snapToRoute(false)
      .build();
    MapboxNavigation navigationWithOptions = buildMapboxNavigationWithOptions(options);

    assertNull(navigationWithOptions.getSnapEngine());
  }

  @Test
  public void fasterRouteEngine_doesNotGetInitializedWithFalseOption() throws Exception {
    MapboxNavigationOptions options = MapboxNavigationOptions.builder()
      .enableFasterRouteDetection(false)
      .build();
    MapboxNavigation navigationWithOptions = buildMapboxNavigationWithOptions(options);

    assertNull(navigationWithOptions.getFasterRouteEngine());
  }

  @Test
  public void addMilestone_milestoneDidGetAdded() throws Exception {
    MapboxNavigation navigation = buildMapboxNavigation();
    Milestone milestone = new StepMilestone.Builder().build();

    navigation.addMilestone(milestone);

    assertTrue(navigation.getMilestones().contains(milestone));
  }

  @Test
  public void addMilestone_milestoneOnlyGetsAddedOnce() throws Exception {
    MapboxNavigationOptions options = MapboxNavigationOptions.builder().defaultMilestonesEnabled(false).build();
    MapboxNavigation navigationWithOptions = buildMapboxNavigationWithOptions(options);

    Milestone milestone = new StepMilestone.Builder().build();
    navigationWithOptions.addMilestone(milestone);
    navigationWithOptions.addMilestone(milestone);

    assertEquals(1, navigationWithOptions.getMilestones().size());
  }

  @Test
  public void removeMilestone_milestoneDidGetRemoved() throws Exception {
    MapboxNavigationOptions options = MapboxNavigationOptions.builder().defaultMilestonesEnabled(false).build();
    MapboxNavigation navigationWithOptions = buildMapboxNavigationWithOptions(options);

    Milestone milestone = new StepMilestone.Builder().build();
    navigationWithOptions.addMilestone(milestone);
    navigationWithOptions.removeMilestone(milestone);

    assertEquals(0, navigationWithOptions.getMilestones().size());
  }

  @Test
  public void removeMilestone_milestoneDoesNotExist() throws Exception {
    MapboxNavigationOptions options = MapboxNavigationOptions.builder().defaultMilestonesEnabled(false).build();
    MapboxNavigation navigationWithOptions = buildMapboxNavigationWithOptions(options);

    Milestone milestone = new StepMilestone.Builder().build();
    navigationWithOptions.addMilestone(new StepMilestone.Builder().build());
    navigationWithOptions.removeMilestone(milestone);

    assertEquals(1, navigationWithOptions.getMilestones().size());
  }

  @Test
  public void removeMilestone_nullRemovesAllMilestones() throws Exception {
    MapboxNavigationOptions options = MapboxNavigationOptions.builder().defaultMilestonesEnabled(false).build();
    MapboxNavigation navigationWithOptions = buildMapboxNavigationWithOptions(options);
    navigationWithOptions.addMilestone(new StepMilestone.Builder().build());
    navigationWithOptions.addMilestone(new StepMilestone.Builder().build());
    navigationWithOptions.addMilestone(new StepMilestone.Builder().build());
    navigationWithOptions.addMilestone(new StepMilestone.Builder().build());

    navigationWithOptions.removeMilestone(null);

    assertEquals(0, navigationWithOptions.getMilestones().size());
  }

  @Test
  public void removeMilestone_correctMilestoneWithIdentifierGetsRemoved() throws Exception {
    MapboxNavigationOptions options = MapboxNavigationOptions.builder().defaultMilestonesEnabled(false).build();
    MapboxNavigation navigationWithOptions = buildMapboxNavigationWithOptions(options);
    int removedMilestoneIdentifier = 5678;
    Milestone milestone = new StepMilestone.Builder().setIdentifier(removedMilestoneIdentifier).build();
    navigationWithOptions.addMilestone(milestone);

    navigationWithOptions.removeMilestone(removedMilestoneIdentifier);

    assertEquals(0, navigationWithOptions.getMilestones().size());
  }

  @Test
  public void removeMilestone_noMilestoneWithIdentifierFound() throws Exception {
    MapboxNavigationOptions options = MapboxNavigationOptions.builder().defaultMilestonesEnabled(false).build();
    MapboxNavigation navigationWithOptions = buildMapboxNavigationWithOptions(options);
    navigationWithOptions.addMilestone(new StepMilestone.Builder().build());
    int removedMilestoneIdentifier = 5678;

    navigationWithOptions.removeMilestone(removedMilestoneIdentifier);

    assertEquals(1, navigationWithOptions.getMilestones().size());
  }

  @Test
  public void getLocationEngine_returnsCorrectLocationEngine() throws Exception {
    MapboxNavigation navigation = buildMapboxNavigation();
    LocationEngine locationEngine = mock(LocationEngine.class);
    LocationEngine locationEngineInstanceNotUsed = mock(LocationEngine.class);

    navigation.setLocationEngine(locationEngine);

    assertNotSame(locationEngineInstanceNotUsed, navigation.getLocationEngine());
    assertEquals(locationEngine, navigation.getLocationEngine());
  }

  @Test
  public void endNavigation_doesSendFalseToNavigationEvent() throws Exception {
    MapboxNavigation navigation = buildMapboxNavigation();
    NavigationEventListener navigationEventListener = mock(NavigationEventListener.class);

    navigation.addNavigationEventListener(navigationEventListener);
    navigation.startNavigation(buildTestDirectionsRoute());
    navigation.endNavigation();

    verify(navigationEventListener, times(1)).onRunning(false);
  }

  @Test
  public void startNavigation_doesSendTrueToNavigationEvent() throws Exception {
    MapboxNavigation navigation = buildMapboxNavigation();
    NavigationEventListener navigationEventListener = mock(NavigationEventListener.class);

    navigation.addNavigationEventListener(navigationEventListener);
    navigation.startNavigation(buildTestDirectionsRoute());

    verify(navigationEventListener, times(1)).onRunning(true);
  }

  @Test
  public void setSnapEngine_doesReplaceDefaultEngine() throws Exception {
    MapboxNavigation navigation = buildMapboxNavigation();

    Snap snap = mock(Snap.class);
    navigation.setSnapEngine(snap);

    assertTrue(!(navigation.getSnapEngine() instanceof SnapToRoute));
  }

  @Test
  public void setOffRouteEngine_doesReplaceDefaultEngine() throws Exception {
    MapboxNavigation navigation = buildMapboxNavigation();

    OffRoute offRoute = mock(OffRoute.class);
    navigation.setOffRouteEngine(offRoute);

    assertTrue(!(navigation.getOffRouteEngine() instanceof OffRouteDetector));
  }

  @Test
  public void getCameraEngine_returnsNonNullEngine() throws Exception {
    MapboxNavigation navigation = buildMapboxNavigation();

    navigation.setOffRouteEngine(null);

    assertNotNull(navigation.getCameraEngine());
  }

  @Test
  public void getCameraEngine_returnsSimpleCameraWhenNull() throws Exception {
    MapboxNavigation navigation = buildMapboxNavigation();

    navigation.setOffRouteEngine(null);

    assertTrue(navigation.getCameraEngine() instanceof SimpleCamera);
  }

  private MapboxNavigation buildMapboxNavigation() {
    return new MapboxNavigation(mock(Context.class), ACCESS_TOKEN, mock(NavigationTelemetry.class),
      mock(LocationEngine.class));
  }

  private MapboxNavigation buildMapboxNavigationWithOptions(MapboxNavigationOptions options) {
    return new MapboxNavigation(mock(Context.class), ACCESS_TOKEN, options, mock(NavigationTelemetry.class),
      mock(LocationEngine.class));
  }
}