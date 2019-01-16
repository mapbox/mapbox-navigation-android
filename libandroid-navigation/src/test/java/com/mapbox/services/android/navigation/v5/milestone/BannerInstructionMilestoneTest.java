package com.mapbox.services.android.navigation.v5.milestone;

import com.mapbox.navigator.BannerComponent;
import com.mapbox.navigator.BannerInstruction;
import com.mapbox.navigator.BannerSection;
import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BannerInstructionMilestoneTest extends BaseTest {

  @Test
  public void checksNotBannerShownIfBannerInstructionIsNull() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    BannerInstruction nullBannerInstruction = null;
    routeProgress = add(nullBannerInstruction, routeProgress);
    BannerInstructionMilestone milestone = buildBannerInstructionMilestone();

    boolean isOccurring = milestone.isOccurring(routeProgress, routeProgress);

    assertFalse(isOccurring);
  }

  @Test
  public void checksBannerShownIfBannerInstructionIsNotNull() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    BannerInstruction emptyBannerInstruction = buildEmptyBannerInstructionWithPrimary(null);
    routeProgress = add(emptyBannerInstruction, routeProgress);
    BannerInstructionMilestone milestone = buildBannerInstructionMilestone();

    boolean isOccurring = milestone.isOccurring(routeProgress, routeProgress);

    assertTrue(isOccurring);
  }

  @Test
  public void checksBannerMappingIfBannerSectionHasComponents() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    int anAbbrPriority = 0;
    boolean isActive = true;
    ArrayList<String> anyDirections = new ArrayList<>();
    anyDirections.add("a direction");
    BannerComponent aComponent = new BannerComponent("a type", "a text", "an abbr", anAbbrPriority,
      "an image base url", isActive, anyDirections);
    ArrayList<BannerComponent> components = new ArrayList<>();
    components.add(aComponent);
    BannerSection sectionWithComponents = new BannerSection("a text", "a type", "a modifier", 60, "a driving side",
      components);
    BannerInstruction emptyBannerInstruction = buildEmptyBannerInstructionWithPrimary(sectionWithComponents);
    routeProgress = add(emptyBannerInstruction, routeProgress);
    BannerInstructionMilestone milestone = buildBannerInstructionMilestone();

    boolean isOccurring = milestone.isOccurring(routeProgress, routeProgress);

    assertTrue(isOccurring);
    assertEquals(1, routeProgress.bannerInstruction().getPrimary().getComponents().size());
  }

  private RouteProgress add(BannerInstruction bannerInstruction, RouteProgress routeProgress) {
    return routeProgress.toBuilder()
      .bannerInstruction(bannerInstruction)
      .build();
  }

  private BannerInstruction buildEmptyBannerInstructionWithPrimary(BannerSection section) {
    return new BannerInstruction(section, null, null, -1, -1);
  }

  private BannerInstructionMilestone buildBannerInstructionMilestone() {
    return (BannerInstructionMilestone) new BannerInstructionMilestone.Builder().setIdentifier(1234).build();
  }
}
