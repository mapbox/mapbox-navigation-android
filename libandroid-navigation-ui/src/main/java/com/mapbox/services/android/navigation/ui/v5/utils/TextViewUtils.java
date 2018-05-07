package com.mapbox.services.android.navigation.ui.v5.utils;

import android.graphics.Paint;
import android.widget.TextView;

public class TextViewUtils {

  public boolean textFits(TextView textView, String text) {
    Paint paint = new Paint(textView.getPaint());
    float width = paint.measureText(text);
    return width < textView.getWidth();
  }
}
