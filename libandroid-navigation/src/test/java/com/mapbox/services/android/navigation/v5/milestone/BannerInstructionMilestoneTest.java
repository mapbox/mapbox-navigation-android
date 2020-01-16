package com.mapbox.services.android.navigation.v5.milestone;

import com.mapbox.api.directions.v5.models.BannerComponents;
import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.api.directions.v5.models.BannerText;
import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BannerInstructionMilestoneTest extends BaseTest {

  @Test
  public void checksNotBannerShownIfBannerInstructionIsNull() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    BannerInstructions nullBannerInstruction = null;
    routeProgress = add(nullBannerInstruction, routeProgress);
    BannerInstructionMilestone milestone = buildBannerInstructionMilestone();

    boolean isOccurring = milestone.isOccurring(routeProgress, routeProgress);

    assertFalse(isOccurring);
  }

  @Test
  public void checksBannerShownIfBannerInstructionIsNotNull() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    BannerText bannerText = mock(BannerText.class);
    when(bannerText.text()).thenReturn("mock text");
    BannerInstructions bannerInstruction =
      buildEmptyBannerInstructionWithPrimary(bannerText);
    routeProgress = add(bannerInstruction, routeProgress);
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
    BannerComponents aComponent = BannerComponents.builder()
            .type("a type")
            .text("a text")
            .abbreviation("an abbr")
            .abbreviationPriority(anAbbrPriority)
            .imageBaseUrl("an image base url")
            .active(isActive)
            .directions(anyDirections)
            .build();
    ArrayList<BannerComponents> components = new ArrayList<>();
    components.add(aComponent);
    BannerText sectionWithComponents = BannerText.builder()
            .text("a text")
            .type("a type")
            .modifier("a modifier")
            .degrees(60.0)
            .drivingSide("a driving side")
            .components(components)
            .build();
    BannerInstructions emptyBannerInstructions = buildEmptyBannerInstructionWithPrimary(sectionWithComponents);
    routeProgress = add(emptyBannerInstructions, routeProgress);
    BannerInstructionMilestone milestone = buildBannerInstructionMilestone();

    boolean isOccurring = milestone.isOccurring(routeProgress, routeProgress);

    assertTrue(isOccurring);
    assertEquals(1, routeProgress.bannerInstruction().primary().components().size());
  }

  private RouteProgress add(BannerInstructions bannerInstructions, RouteProgress routeProgress) {
    return routeProgress.toBuilder()
      .bannerInstruction(bannerInstructions)
      .build();
  }

  private BannerInstructions buildEmptyBannerInstructionWithPrimary(BannerText text) {
    return BannerInstructions.builder()
            .primary(text)
            .distanceAlongGeometry(0.3f)
            .build();
  }

  private BannerInstructionMilestone buildBannerInstructionMilestone() {
    return (BannerInstructionMilestone) new BannerInstructionMilestone.Builder().setIdentifier(1234).build();
  }
}
