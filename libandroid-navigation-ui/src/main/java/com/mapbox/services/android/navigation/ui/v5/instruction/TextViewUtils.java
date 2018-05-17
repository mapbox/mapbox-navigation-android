package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.graphics.Paint;
import android.widget.TextView;

class TextViewUtils {

  boolean textFits(TextView textView, String text) {
    Paint paint = new Paint(textView.getPaint());
    float width = paint.measureText(text);
    return width < textView.getWidth();
  }
}
