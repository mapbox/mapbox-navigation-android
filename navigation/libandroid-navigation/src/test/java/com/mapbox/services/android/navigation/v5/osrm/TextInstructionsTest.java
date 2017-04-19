package com.mapbox.services.android.navigation.v5.osrm;

import com.google.gson.Gson;
import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.services.api.directions.v5.models.IntersectionLanes;
import com.mapbox.services.api.directions.v5.models.LegStep;
import com.mapbox.services.api.directions.v5.models.StepIntersection;
import com.mapbox.services.api.directions.v5.models.StepManeuver;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TextInstructionsTest extends BaseTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testSanity() {
    TextInstructions textInstructions = new TextInstructions("en", "v5");
    assertNotNull(textInstructions.getVersionObject());
  }

  @Test
  public void testBadLanguage() {
    thrown.expect(RuntimeException.class);
    thrown.expectMessage(startsWith("Translation not found for language: xxx"));
    new TextInstructions("xxx", "v5");
  }

  @Test
  public void testBadVersion() {
    thrown.expect(RuntimeException.class);
    thrown.expectMessage(startsWith("Version not found for value: yyy"));
    new TextInstructions("en", "yyy");
  }

  @Test
  public void testCapitalizeFirstLetter() {
    assertEquals("Mapbox", TextInstructions.capitalizeFirstLetter("mapbox"));
  }

  @Test
  public void testOrdinalize() {
    TextInstructions textInstructions = new TextInstructions("en", "v5");
    assertEquals("1st", textInstructions.ordinalize(1));
    assertEquals("", textInstructions.ordinalize(999));
  }

  @Test
  public void testValidDirectionFromDegree() {
    TextInstructions textInstructions = new TextInstructions("en", "v5");
    assertEquals("", textInstructions.directionFromDegree(null));
    assertEquals("north", textInstructions.directionFromDegree(0.));
    assertEquals("north", textInstructions.directionFromDegree(1.));
    assertEquals("north", textInstructions.directionFromDegree(20.));
    assertEquals("northeast", textInstructions.directionFromDegree(21.));
    assertEquals("northeast", textInstructions.directionFromDegree(69.));
    assertEquals("east", textInstructions.directionFromDegree(70.));
    assertEquals("east", textInstructions.directionFromDegree(110.));
    assertEquals("southeast", textInstructions.directionFromDegree(111.));
    assertEquals("southeast", textInstructions.directionFromDegree(159.));
    assertEquals("south", textInstructions.directionFromDegree(160.));
    assertEquals("south", textInstructions.directionFromDegree(200.));
    assertEquals("southwest", textInstructions.directionFromDegree(201.));
    assertEquals("southwest", textInstructions.directionFromDegree(249.));
    assertEquals("west", textInstructions.directionFromDegree(250.));
    assertEquals("west", textInstructions.directionFromDegree(290.));
    assertEquals("northwest", textInstructions.directionFromDegree(291.));
    assertEquals("northwest", textInstructions.directionFromDegree(339.));
    assertEquals("north", textInstructions.directionFromDegree(340.));
    assertEquals("north", textInstructions.directionFromDegree(360.));
  }

  @Test
  public void testInvalidDirectionFromDegree() {
    TextInstructions textInstructions = new TextInstructions("en", "v5");
    thrown.expect(RuntimeException.class);
    thrown.expectMessage(startsWith("Degree is invalid: 361"));
    assertEquals("", textInstructions.directionFromDegree(361.));
  }

  @Test
  public void testLaneDiagram() {
    TextInstructions textInstructions = new TextInstructions("en", "v5");
    Map<String, LegStep> map = new HashMap<>();

    map.put("o", new LegStep(Collections.singletonList(new StepIntersection(new IntersectionLanes[] {
      new IntersectionLanes(true), new IntersectionLanes(true), new IntersectionLanes(true)}))));
    map.put("ox", new LegStep(Collections.singletonList(new StepIntersection(new IntersectionLanes[] {
      new IntersectionLanes(true), new IntersectionLanes(true), new IntersectionLanes(false)}))));
    map.put("ox", new LegStep(Collections.singletonList(new StepIntersection(new IntersectionLanes[] {
      new IntersectionLanes(true), new IntersectionLanes(true), new IntersectionLanes(false),
      new IntersectionLanes(false)}))));
    map.put("oxo", new LegStep(Collections.singletonList(new StepIntersection(new IntersectionLanes[] {
      new IntersectionLanes(true), new IntersectionLanes(false), new IntersectionLanes(true)}))));
    map.put("xox", new LegStep(Collections.singletonList(new StepIntersection(new IntersectionLanes[] {
      new IntersectionLanes(false), new IntersectionLanes(true), new IntersectionLanes(true),
      new IntersectionLanes(false)}))));
    map.put("xoxox", new LegStep(Collections.singletonList(new StepIntersection(new IntersectionLanes[] {
      new IntersectionLanes(false), new IntersectionLanes(true), new IntersectionLanes(false),
      new IntersectionLanes(true), new IntersectionLanes(false)}))));
    map.put("x", new LegStep(Collections.singletonList(new StepIntersection(new IntersectionLanes[] {
      new IntersectionLanes(false), new IntersectionLanes(false), new IntersectionLanes(false)}))));

    for (Object entry : map.entrySet()) {
      Map.Entry pair = (Map.Entry) entry;
      assertEquals(pair.getKey(), textInstructions.laneConfig((LegStep) pair.getValue()));
    }
  }

  @Test
  public void testInvalidLaneDiagram() {
    TextInstructions textInstructions = new TextInstructions("en", "v5");

    thrown.expect(RuntimeException.class);
    thrown.expectMessage(startsWith("No lanes object"));
    assertEquals("", textInstructions.laneConfig(new LegStep(Collections.singletonList(
      new StepIntersection(new IntersectionLanes[] {})))));
  }

  @Test
  public void respectInstructionHook() {
    TextInstructions textInstructions = new TextInstructions("en", "v5");
    textInstructions.setTokenizedInstructionHook(new TokenizedInstructionHook() {
      @Override
      public String change(String instruction) {
        return instruction.replace("{way_name}", "<blink>{way_name}</blink>");
      }
    });

    LegStep step = new LegStep("Way Name", "", new StepManeuver("turn", "left", null));
    assertEquals("Turn left onto <blink>Way Name</blink>", textInstructions.compile(step));
  }

  @Test
  public void testFixturesMatchGeneratedInstructions() throws IOException {
    for (String fixture : TextInstructionsFixtures.FIXTURES) {
      String body = readPath(fixture);
      FixtureModel model = new Gson().fromJson(body, FixtureModel.class);
      for (Object entry : model.getInstructions().entrySet()) {
        Map.Entry pair = (Map.Entry) entry;
        String language = (String) pair.getKey();
        String compiled = (String) pair.getValue();
        assertEquals(compiled, new TextInstructions(language, "v5").compile(model.getStep()));
      }
    }
  }

  private class FixtureModel {
    private LegStep step;
    private Map<String, String> instructions;

    public FixtureModel() {
    }

    public LegStep getStep() {
      return step;
    }

    public Map<String, String> getInstructions() {
      return instructions;
    }
  }
}
