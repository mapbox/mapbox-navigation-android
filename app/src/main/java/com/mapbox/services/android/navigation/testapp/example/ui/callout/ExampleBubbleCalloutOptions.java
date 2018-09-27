package com.mapbox.services.android.navigation.testapp.example.ui.callout;

import android.graphics.Color;

public class ExampleBubbleCalloutOptions extends ExampleCalloutOptions {

  private float strokeWidth = 2;
  private int strokeColor = Color.DKGRAY;
  private float cornerRadius = 12;
  private float arrowHeight = 16;
  private float arrowWidth = 16;

  public ExampleBubbleCalloutOptions withStrokeWidth(float strokeWidth) {
    this.strokeWidth = strokeWidth;
    return this;
  }

  public ExampleBubbleCalloutOptions withStrokeColor(int strokeColor) {
    this.strokeColor = strokeColor;
    return this;
  }

  public ExampleBubbleCalloutOptions withCornerRadius(float cornerRadius) {
    this.cornerRadius = cornerRadius;
    return this;
  }

  public ExampleBubbleCalloutOptions withArrowWidth(float arrowWidth) {
    this.arrowWidth = arrowWidth;
    return this;
  }

  public ExampleBubbleCalloutOptions withArrowHeight(float arrowHeight) {
    this.arrowHeight = arrowHeight;
    return this;
  }

  @Override
  ExampleCallout build(long id) {
    validateInput();
    return new ExampleBubbleCallout(
      generateId(id), text, point, textColor, backgroundColor,
      textSize, padding, strokeWidth, strokeColor,
      cornerRadius, arrowWidth, arrowHeight
    );
  }
}