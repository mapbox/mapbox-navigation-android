package com.mapbox.services.android.navigation.v5.utils.span;

public class SpanItem {

  private Object span;
  private String spanText;

  public SpanItem(Object span, String spanText) {
    this.span = span;
    this.spanText = spanText;
  }

  public Object getSpan() {
    return span;
  }

  public String getSpanText() {
    return spanText;
  }
}
