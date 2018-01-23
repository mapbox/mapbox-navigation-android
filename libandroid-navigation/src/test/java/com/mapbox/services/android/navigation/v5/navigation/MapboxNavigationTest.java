package com.mapbox.services.android.navigation.v5.navigation;

import android.content.Context;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.services.android.navigation.v5.milestone.Milestone;
import com.mapbox.services.android.navigation.v5.milestone.StepMilestone;
import com.mapbox.services.android.navigation.v5.offroute.OffRoute;
import com.mapbox.services.android.navigation.v5.offroute.OffRouteDetector;
import com.mapbox.services.android.navigation.v5.snap.Snap;
import com.mapbox.services.android.navigation.v5.snap.SnapToRoute;
import com.mapbox.services.android.telemetry.location.LocationEngine;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.BANNER_INSTRUCTION_MILESTONE_ID;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.VOICE_INSTRUCTION_MILESTONE_ID;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class MapboxNavigationTest extends BaseTest {

  private MapboxNavigation navigation;

  @Before
  public void setUp() throws Exception {
    navigation = new MapboxNavigation(mock(Context.class), ACCESS_TOKEN, mock(NavigationTelemetry.class),
      mock(LocationEngine.class));
  }

  @Test
  public void sanityTest() {
    assertNotNull("should not be null", navigation);
    MapboxNavigationOptions options = MapboxNavigationOptions.builder().build();
    MapboxNavigation navigationWithOptions = new MapboxNavigation(mock(Context.class),
      ACCESS_TOKEN, options, mock(NavigationTelemetry.class), mock(LocationEngine.class));
    assertNotNull("should not be null", navigationWithOptions);
  }

  @Test
  public void voiceMilestone_onInitializationDoesGetAdded() throws Exception {
    assertTrue(navigation.getMilestones().get(0).getIdentifier() == VOICE_INSTRUCTION_MILESTONE_ID);
  }

  @Test
  public void bannerMilestone_onInitializationDoesGetAdded() throws Exception {
    assertTrue(navigation.getMilestones().get(1).getIdentifier() == BANNER_INSTRUCTION_MILESTONE_ID);
  }


  @Test
  public void defaultMilestones_onInitializationDoNotGetAdded() throws Exception {
    MapboxNavigationOptions options = MapboxNavigationOptions.builder().defaultMilestonesEnabled(false).build();
    MapboxNavigation navigationWithOptions = new MapboxNavigation(mock(Context.class),
      ACCESS_TOKEN, options, mock(NavigationTelemetry.class), mock(LocationEngine.class));
    assertEquals(0, navigationWithOptions.getMilestones().size());
  }

  @Test
  public void defaultEngines_didGetInitialized() throws Exception {
    assertNotNull(navigation.getSnapEngine());
    assertNotNull(navigation.getOffRouteEngine());
  }

  @Test
  public void addMilestone_milestoneDidGetAdded() throws Exception {
    Milestone milestone = new StepMilestone.Builder().build();
    navigation.addMilestone(milestone);
    assertTrue(navigation.getMilestones().contains(milestone));
  }

  @Test
  public void addMilestone_milestoneOnlyGetsAddedOnce() throws Exception {
    MapboxNavigationOptions options = MapboxNavigationOptions.builder().defaultMilestonesEnabled(false).build();
    MapboxNavigation navigationWithOptions = new MapboxNavigation(mock(Context.class),
      ACCESS_TOKEN, options, mock(NavigationTelemetry.class), mock(LocationEngine.class));
    Milestone milestone = new StepMilestone.Builder().build();
    navigationWithOptions.addMilestone(milestone);
    navigationWithOptions.addMilestone(milestone);
    assertEquals(1, navigationWithOptions.getMilestones().size());
  }

  @Test
  public void removeMilestone_milestoneDidGetRemoved() throws Exception {
    MapboxNavigationOptions options = MapboxNavigationOptions.builder().defaultMilestonesEnabled(false).build();
    MapboxNavigation navigationWithOptions = new MapboxNavigation(mock(Context.class),
      ACCESS_TOKEN, options, mock(NavigationTelemetry.class), mock(LocationEngine.class));
    Milestone milestone = new StepMilestone.Builder().build();
    navigationWithOptions.addMilestone(milestone);
    assertEquals(1, navigationWithOptions.getMilestones().size());
    navigationWithOptions.removeMilestone(milestone);
    assertEquals(0, navigationWithOptions.getMilestones().size());
  }

  @Test
  public void removeMilestone_milestoneDoesNotExist() throws Exception {
    MapboxNavigationOptions options = MapboxNavigationOptions.builder().defaultMilestonesEnabled(false).build();
    MapboxNavigation navigationWithOptions = new MapboxNavigation(mock(Context.class),
      ACCESS_TOKEN, options, mock(NavigationTelemetry.class), mock(LocationEngine.class));
    Milestone milestone = new StepMilestone.Builder().build();
    navigationWithOptions.addMilestone(new StepMilestone.Builder().build());
    navigationWithOptions.removeMilestone(milestone);
    assertEquals(1, navigationWithOptions.getMilestones().size());
  }

  @Test
  public void removeMilestone_nullRemovesAllMilestones() throws Exception {
    MapboxNavigationOptions options = MapboxNavigationOptions.builder().defaultMilestonesEnabled(false).build();
    MapboxNavigation navigationWithOptions = new MapboxNavigation(mock(Context.class),
      ACCESS_TOKEN, options, mock(NavigationTelemetry.class), mock(LocationEngine.class));
    navigationWithOptions.addMilestone(new StepMilestone.Builder().build());
    navigationWithOptions.addMilestone(new StepMilestone.Builder().build());
    navigationWithOptions.addMilestone(new StepMilestone.Builder().build());
    navigationWithOptions.addMilestone(new StepMilestone.Builder().build());
    assertEquals(4, navigationWithOptions.getMilestones().size());
    navigationWithOptions.removeMilestone(null);
    assertEquals(0, navigationWithOptions.getMilestones().size());
  }

  @Test
  public void removeMilestone_correctMilestoneWithIdentifierGetsRemoved() throws Exception {
    MapboxNavigationOptions options = MapboxNavigationOptions.builder().defaultMilestonesEnabled(false).build();
    MapboxNavigation navigationWithOptions = new MapboxNavigation(mock(Context.class),
      ACCESS_TOKEN, options, mock(NavigationTelemetry.class), mock(LocationEngine.class));
    Milestone milestone = new StepMilestone.Builder().setIdentifier(5678).build();
    navigationWithOptions.addMilestone(milestone);
    assertEquals(1, navigationWithOptions.getMilestones().size());
    navigationWithOptions.removeMilestone(5678);
    assertEquals(0, navigationWithOptions.getMilestones().size());
  }

  @Test
  public void removeMilestone_noMilestoneWithIdentifierFound() throws Exception {
    MapboxNavigationOptions options = MapboxNavigationOptions.builder().defaultMilestonesEnabled(false).build();
    MapboxNavigation navigationWithOptions = new MapboxNavigation(mock(Context.class),
      ACCESS_TOKEN, options, mock(NavigationTelemetry.class), mock(LocationEngine.class));
    navigationWithOptions.addMilestone(new StepMilestone.Builder().build());
    assertEquals(1, navigationWithOptions.getMilestones().size());
    navigationWithOptions.removeMilestone(5678);
    assertEquals(1, navigationWithOptions.getMilestones().size());
  }

  @Test
  public void getLocationEngine_returnsCorrectLocationEngine() throws Exception {
    LocationEngine locationEngine = mock(LocationEngine.class);
    LocationEngine locationEngine2 = mock(LocationEngine.class);
    navigation.setLocationEngine(locationEngine);
    assertNotSame(locationEngine2, navigation.getLocationEngine());
    assertEquals(locationEngine, navigation.getLocationEngine());
  }

  @Test
  @Ignore
  public void endNavigation_doesSendFalseToNavigationEvent() throws Exception {
    NavigationEventListener navigationEventListener = mock(NavigationEventListener.class);
    navigation.addNavigationEventListener(navigationEventListener);
    navigation.startNavigation(mock(DirectionsRoute.class));
    navigation.endNavigation();
    verify(navigationEventListener, times(1)).onRunning(false);
  }

  @Test
  public void startNavigation_doesSendTrueToNavigationEvent() throws Exception {
    NavigationEventListener navigationEventListener = mock(NavigationEventListener.class);
    navigation.addNavigationEventListener(navigationEventListener);
    navigation.startNavigation(mock(DirectionsRoute.class));
    verify(navigationEventListener, times(1)).onRunning(true);
  }

  @Test
  public void setSnapEngine_doesReplaceDefaultEngine() throws Exception {
    Snap snap = navigation.getSnapEngine();
    assertTrue(snap instanceof SnapToRoute);
    snap = mock(Snap.class);
    navigation.setSnapEngine(snap);
    assertTrue(!(navigation.getSnapEngine() instanceof SnapToRoute));
    assertTrue(navigation.getSnapEngine() instanceof Snap);
  }

  @Test
  public void setOffRouteEngine_doesReplaceDefaultEngine() throws Exception {
    OffRoute offRoute = navigation.getOffRouteEngine();
    assertTrue(offRoute instanceof OffRouteDetector);
    offRoute = mock(OffRoute.class);
    navigation.setOffRouteEngine(offRoute);
    assertTrue(!(navigation.getOffRouteEngine() instanceof OffRouteDetector));
    assertTrue(navigation.getOffRouteEngine() instanceof OffRoute);
  }
}