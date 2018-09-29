package com.mapbox.services.android.navigation.testapp.example.ui.callout;

import android.graphics.Color;
import android.support.annotation.ColorInt;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.geometry.LatLng;

public class ExampleCalloutOptions {

  private static final String ID_TEMPLATE = "callout_%s";

  protected String text;
  protected Point point;
  protected int textColor = Color.WHITE;
  protected int backgroundColor = Color.BLACK;
  protected float textSize = 12.0f;
  protected int[] padding = new int[] {12, 8, 12, 8};

  public ExampleCalloutOptions withText(String text) {
    this.text = text;
    return this;
  }

  public ExampleCalloutOptions withLatLng(LatLng latLng) {
    this.point = Point.fromLngLat(latLng.getLongitude(), latLng.getLatitude());
    return this;
  }

  public ExampleCalloutOptions withGeometry(Point point) {
    this.point = point;
    return this;
  }

  public ExampleCalloutOptions withTextColor(@ColorInt int textColor) {
    this.textColor = textColor;
    return this;
  }

  public ExampleCalloutOptions withBackgroundColor(@ColorInt int backgroundColor) {
    this.backgroundColor = backgroundColor;
    return this;
  }

  public ExampleCalloutOptions withTextSize(float textSize) {
    this.textSize = textSize;
    return this;
  }

  public ExampleCalloutOptions withPadding(int[] padding) {
    this.padding = padding;
    return this;
  }

  protected void validateInput() {
    if (point == null) {
      throw new IllegalArgumentException("LatLng is required to build a callout");
    }

    if (text == null) {
      throw new IllegalArgumentException("Text is required to build a callout");
    }
  }

  protected String generateId(long baseId) {
    return String.format(ID_TEMPLATE, baseId);
  }

  ExampleCallout build(long id) {
    validateInput();
    return new ExampleCallout(generateId(id), text, point, textColor, backgroundColor, textSize, padding);
  }
}
