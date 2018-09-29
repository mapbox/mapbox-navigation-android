package com.mapbox.services.android.navigation.testapp.example.ui.callout;

import com.google.gson.JsonObject;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;

public class ExampleCallout {

  public static final String KEY_ID = "callout_id";
  private static final String KEY_TEXT = "callout_text";
  private static final String KEY_TEXT_COLOR = "callout_textcolor";
  private static final String KEY_BG_COLOR = "callout_bgcolor";
  private static final String KEY_TEXT_SIZE = "callout_size";
  private static final String KEY_PADDING_LEFT = "callout_padding_left";
  private static final String KEY_PADDING_TOP = "callout_padding_top";
  private static final String KEY_PADDING_RIGHT = "callout_padding_right";
  private static final String KEY_PADDING_BOTTOM = "callout_padding_bottom";

  private final Point point;
  protected final JsonObject jsonObject = new JsonObject();

  ExampleCallout(String id, String text, Point point, int textColor,
                 int backgroundColor, float textSize, int[] padding) {
    this.point = point;
    jsonObject.addProperty(KEY_ID, id);
    jsonObject.addProperty(KEY_TEXT, text);
    jsonObject.addProperty(KEY_TEXT_COLOR, textColor);
    jsonObject.addProperty(KEY_BG_COLOR, backgroundColor);
    jsonObject.addProperty(KEY_TEXT_SIZE, textSize);
    jsonObject.addProperty(KEY_PADDING_LEFT, padding[0]);
    jsonObject.addProperty(KEY_PADDING_TOP, padding[1]);
    jsonObject.addProperty(KEY_PADDING_RIGHT, padding[2]);
    jsonObject.addProperty(KEY_PADDING_BOTTOM, padding[3]);
  }

  public String getId() {
    return jsonObject.get(KEY_ID).getAsString();
  }

  public String getText() {
    return jsonObject.get(KEY_TEXT).getAsString();
  }

  public int getTextColor() {
    return jsonObject.get(KEY_TEXT_COLOR).getAsInt();
  }

  public int getBackgroundColor() {
    return jsonObject.get(KEY_BG_COLOR).getAsInt();
  }

  public float getTextSize() {
    return jsonObject.get(KEY_TEXT_SIZE).getAsFloat();
  }

  public int[] getPadding() {
    return new int[] {
      jsonObject.get(KEY_PADDING_LEFT).getAsInt(),
      jsonObject.get(KEY_PADDING_TOP).getAsInt(),
      jsonObject.get(KEY_PADDING_RIGHT).getAsInt(),
      jsonObject.get(KEY_PADDING_BOTTOM).getAsInt()
    };
  }

  Feature toFeature() {
    return Feature.fromGeometry(point, jsonObject);
  }
}
