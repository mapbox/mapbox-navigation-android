package com.mapbox.services.android.navigation.v5.osrm;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mapbox.services.Experimental;
import com.mapbox.services.api.directions.v5.models.IntersectionLanes;
import com.mapbox.services.api.directions.v5.models.LegStep;
import com.mapbox.services.commons.utils.TextUtils;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Text instructions from OSRM route responses
 * <p>
 * This is an experimental API. Experimental APIs are quickly evolving and
 * might change or be removed in minor versions.
 *
 * @since 2.0.0
 */
@Experimental
public class TextInstructions {

  private static final Logger logger = Logger.getLogger(TextInstructions.class.getSimpleName());

  private TokenizedInstructionHook tokenizedInstructionHook;

  private JsonObject rootObject;
  private JsonObject versionObject;

  public TextInstructions(String language, String version) {
    // Load the resource
    String localPath = String.format("translations/%s.json", language);
    InputStream resource = getClass().getClassLoader().getResourceAsStream(localPath);
    if (resource == null) {
      throw new RuntimeException("Translation not found for language: " + language);
    }

    // Parse the JSON content
    rootObject = new JsonParser().parse(new InputStreamReader(resource)).getAsJsonObject();
    versionObject = rootObject.getAsJsonObject(version);
    if (versionObject == null) {
      throw new RuntimeException("Version not found for value: " + version);
    }
  }

  public TokenizedInstructionHook getTokenizedInstructionHook() {
    return tokenizedInstructionHook;
  }

  public void setTokenizedInstructionHook(TokenizedInstructionHook tokenizedInstructionHook) {
    this.tokenizedInstructionHook = tokenizedInstructionHook;
  }

  public JsonObject getRootObject() {
    return rootObject;
  }

  public JsonObject getVersionObject() {
    return versionObject;
  }

  public static String capitalizeFirstLetter(String text) {
    return text.substring(0, 1).toUpperCase() + text.substring(1);
  }

  /**
   * Transform numbers to their translated ordinalized value
   *
   * @param number value
   * @return translated ordinalized value
   */
  public String ordinalize(Integer number) {
    try {
      return getVersionObject().getAsJsonObject("constants").getAsJsonObject("ordinalize")
        .getAsJsonPrimitive(String.valueOf(number)).getAsString();
    } catch (Exception exception) {
      return "";
    }
  }

  /**
   * Transform degrees to their translated compass direction
   *
   * @param degree value
   * @return translated compass direction
   */
  public String directionFromDegree(Double degree) {
    if (degree == null) {
      // step had no bearing_after degree, ignoring
      return "";
    } else if (degree >= 0 && degree <= 20) {
      return getVersionObject().getAsJsonObject("constants").getAsJsonObject("direction")
        .getAsJsonPrimitive("north").getAsString();
    } else if (degree > 20 && degree < 70) {
      return getVersionObject().getAsJsonObject("constants").getAsJsonObject("direction")
        .getAsJsonPrimitive("northeast").getAsString();
    } else if (degree >= 70 && degree <= 110) {
      return getVersionObject().getAsJsonObject("constants").getAsJsonObject("direction")
        .getAsJsonPrimitive("east").getAsString();
    } else if (degree > 110 && degree < 160) {
      return getVersionObject().getAsJsonObject("constants").getAsJsonObject("direction")
        .getAsJsonPrimitive("southeast").getAsString();
    } else if (degree >= 160 && degree <= 200) {
      return getVersionObject().getAsJsonObject("constants").getAsJsonObject("direction")
        .getAsJsonPrimitive("south").getAsString();
    } else if (degree > 200 && degree < 250) {
      return getVersionObject().getAsJsonObject("constants").getAsJsonObject("direction")
        .getAsJsonPrimitive("southwest").getAsString();
    } else if (degree >= 250 && degree <= 290) {
      return getVersionObject().getAsJsonObject("constants").getAsJsonObject("direction")
        .getAsJsonPrimitive("west").getAsString();
    } else if (degree > 290 && degree < 340) {
      return getVersionObject().getAsJsonObject("constants").getAsJsonObject("direction")
        .getAsJsonPrimitive("northwest").getAsString();
    } else if (degree >= 340 && degree <= 360) {
      return getVersionObject().getAsJsonObject("constants").getAsJsonObject("direction")
        .getAsJsonPrimitive("north").getAsString();
    } else {
      throw new RuntimeException("Degree is invalid: " + degree);
    }
  }

  /**
   * Reduce any lane combination down to a contracted lane diagram
   *
   * @param step a route step
   */
  public String laneConfig(LegStep step) {
    if (step.getIntersections() == null
      || step.getIntersections().size() == 0
      || step.getIntersections().get(0).getLanes() == null
      || step.getIntersections().get(0).getLanes().length == 0) {
      throw new RuntimeException("No lanes object");
    }

    StringBuilder config = new StringBuilder();
    Boolean currentLaneValidity = null;
    for (IntersectionLanes lane : step.getIntersections().get(0).getLanes()) {
      if (currentLaneValidity == null || currentLaneValidity != lane.getValid()) {
        if (lane.getValid()) {
          config.append("o");
        } else {
          config.append("x");
        }
        currentLaneValidity = lane.getValid();
      }
    }

    return config.toString();
  }

  public String compile(LegStep step) {
    if (step.getManeuver() == null) {
      throw new RuntimeException("No step maneuver provided.");
    }

    String type = step.getManeuver().getType();
    String modifier = step.getManeuver().getModifier();
    String mode = step.getMode();

    if (TextUtils.isEmpty(type)) {
      throw new RuntimeException("Missing step maneuver type.");
    }

    if (!type.equals("depart") && !type.equals("arrive") && TextUtils.isEmpty(modifier)) {
      throw new RuntimeException("Missing step maneuver modifier.");
    }

    if (getVersionObject().getAsJsonObject(type) == null) {
      // Log for debugging
      logger.log(Level.FINE, "Encountered unknown instruction type: " + type);

      // OSRM specification assumes turn types can be added without
      // major version changes. Unknown types are to be treated as
      // type `turn` by clients
      type = "turn";
    }

    // Use special instructions if available, otherwise `defaultinstruction`
    JsonObject instructionObject;
    JsonObject modeValue = getVersionObject().getAsJsonObject("modes").getAsJsonObject(mode);
    if (modeValue != null) {
      instructionObject = modeValue;
    } else {
      JsonObject modifierValue = getVersionObject().getAsJsonObject(type).getAsJsonObject(modifier);
      instructionObject = modifierValue == null
        ? getVersionObject().getAsJsonObject(type).getAsJsonObject("default")
        : modifierValue;
    }

    // Special case handling
    JsonPrimitive laneInstruction = null;
    switch (type) {
      case "use lane":
        laneInstruction = getVersionObject().getAsJsonObject("constants")
          .getAsJsonObject("lanes").getAsJsonPrimitive(laneConfig(step));
        if (laneInstruction == null) {
          // If the lane combination is not found, default to continue straight
          instructionObject = getVersionObject().getAsJsonObject("use lane")
            .getAsJsonObject("no_lanes");
        }
        break;
      case "rotary":
      case "roundabout":
        if (!TextUtils.isEmpty(step.getRotaryName())
          && step.getManeuver().getExit() != null
          && instructionObject.getAsJsonObject("name_exit") != null) {
          instructionObject = instructionObject.getAsJsonObject("name_exit");
        } else if (step.getRotaryName() != null && instructionObject.getAsJsonObject("name") != null) {
          instructionObject = instructionObject.getAsJsonObject("name");
        } else if (step.getManeuver().getExit() != null && instructionObject.getAsJsonObject("exit") != null) {
          instructionObject = instructionObject.getAsJsonObject("exit");
        } else {
          instructionObject = instructionObject.getAsJsonObject("default");
        }
        break;
      default:
        // NOOP, since no special logic for that type
    }

    // Decide way_name with special handling for name and ref
    String wayName;
    String name = TextUtils.isEmpty(step.getName()) ? "" : step.getName();
    String ref = TextUtils.isEmpty(step.getRef()) ? "" : step.getRef().split(";")[0];

    // Remove hacks from Mapbox Directions mixing ref into name
    if (name.equals(step.getRef())) {
      // if both are the same we assume that there used to be an empty name, with the ref being filled in for it
      // we only need to retain the ref then
      name = "";
    }
    name = name.replace(" (" + step.getRef() + ")", "");

    if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(ref) && !name.equals(ref)) {
      wayName = name + " (" + ref + ")";
    } else if (TextUtils.isEmpty(name) && !TextUtils.isEmpty(ref)) {
      wayName = ref;
    } else {
      wayName = name;
    }

    // Decide which instruction string to use
    // Destination takes precedence over name
    String instruction;
    if (!TextUtils.isEmpty(step.getDestinations())
      && instructionObject.getAsJsonPrimitive("destination") != null) {
      instruction = instructionObject.getAsJsonPrimitive("destination").getAsString();
    } else if (!TextUtils.isEmpty(wayName)
      && instructionObject.getAsJsonPrimitive("name") != null) {
      instruction = instructionObject.getAsJsonPrimitive("name").getAsString();
    } else {
      instruction = instructionObject.getAsJsonPrimitive("default").getAsString();
    }

    if (getTokenizedInstructionHook() != null) {
      instruction = getTokenizedInstructionHook().change(instruction);
    }

    // Replace tokens
    // NOOP if they don't exist
    String nthWaypoint = ""; // TODO, add correct waypoint counting
    JsonPrimitive modifierValue =
      getVersionObject().getAsJsonObject("constants").getAsJsonObject("modifier").getAsJsonPrimitive(modifier);
    instruction = instruction
      .replace("{way_name}", wayName)
      .replace("{destination}", TextUtils.isEmpty(step.getDestinations()) ? "" : step.getDestinations().split(",")[0])
      .replace("{exit_number}",
        step.getManeuver().getExit() == null ? ordinalize(1) : ordinalize(step.getManeuver().getExit()))
      .replace("{rotary_name}", TextUtils.isEmpty(step.getRotaryName()) ? "" : step.getRotaryName())
      .replace("{lane_instruction}", laneInstruction == null ? "" : laneInstruction.getAsString())
      .replace("{modifier}", modifierValue == null ? "" : modifierValue.getAsString())
      .replace("{direction}", directionFromDegree(step.getManeuver().getBearingAfter()))
      .replace("{nth}", nthWaypoint)
      .replaceAll("\\s+", " "); // remove excess spaces

    if (getRootObject().getAsJsonObject("meta").getAsJsonPrimitive("capitalizeFirstLetter").getAsBoolean()) {
      instruction = capitalizeFirstLetter(instruction);
    }

    return instruction;
  }
}
