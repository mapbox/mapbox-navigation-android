package com.mapbox.services.android.navigation.v5.utils.span;

import android.graphics.drawable.Drawable;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ImageSpan;

import java.io.InputStream;
import java.net.URL;

public class ImageSpanItem implements SpanItem {

  private static final String IMAGE_SPAN_SRC_NAME = "image_span_src_name";

  private Object span;

  public ImageSpanItem(String url) {
    buildImageSpan(url);
  }

  @Override
  public Object getSpan() {
    return span;
  }

  private void buildImageSpan(String url) {
    span = new ImageSpan(loadImage(url), DynamicDrawableSpan.ALIGN_BASELINE);
  }

  private static Drawable loadImage(String url) {
    try {
      InputStream stream = (InputStream) new URL(url).getContent();
      return Drawable.createFromStream(stream, IMAGE_SPAN_SRC_NAME);
    } catch (Exception e) {
      return null;
    }
  }
}
