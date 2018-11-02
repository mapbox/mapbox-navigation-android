package com.mapbox.services.android.navigation.testapp.example.ui.callout;

import com.mapbox.geojson.Point;

public class ExampleBubbleCallout extends ExampleCallout {

  private static final String KEY_STROKE_WIDTH = "callout_stroke_width";
  private static final String KEY_STROKE_COLOR = "callout_stroke_color";
  private static final String KEY_CORNER_RADIUS = "callout_radius";
  private static final String KEY_ARROW_WIDTH = "callout_arrow_width";
  private static final String KEY_ARROW_HEIGHT = "callout_arrow_height";

  ExampleBubbleCallout(String id, String text, Point point, int textColor, int backgroundColor, float textSize,
                       int[] padding, float strokeWidth, int strokeColor, float cornerRadius, float arrowWidth,
                       float arrowHeight) {
    super(id, text, point, textColor, backgroundColor, textSize, padding);
    jsonObject.addProperty(KEY_STROKE_WIDTH, strokeWidth);
    jsonObject.addProperty(KEY_STROKE_COLOR, strokeColor);
    jsonObject.addProperty(KEY_CORNER_RADIUS, cornerRadius);
    jsonObject.addProperty(KEY_ARROW_WIDTH, arrowWidth);
    jsonObject.addProperty(KEY_ARROW_HEIGHT, arrowHeight);
  }

  public float getStrokeWidth() {
    return jsonObject.get(KEY_STROKE_WIDTH).getAsFloat();
  }

  public int getStrokeColor() {
    return jsonObject.get(KEY_STROKE_COLOR).getAsInt();
  }

  public float getCornerRadius() {
    return jsonObject.get(KEY_CORNER_RADIUS).getAsFloat();
  }

  public float getArrowHeight() {
    return jsonObject.get(KEY_ARROW_HEIGHT).getAsFloat();
  }

  public float getArrowWidth() {
    return jsonObject.get(KEY_ARROW_WIDTH).getAsFloat();
  }
}
