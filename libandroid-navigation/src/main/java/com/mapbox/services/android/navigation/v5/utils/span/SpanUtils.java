package com.mapbox.services.android.navigation.v5.utils.span;

import android.os.Build;
import android.text.Spannable;
import android.text.SpannableStringBuilder;

import java.util.List;

public class SpanUtils {

  public static SpannableStringBuilder combineSpans(List<SpanItem> spanItems) {
    SpannableStringBuilder builder = new SpannableStringBuilder("");
    for (SpanItem spanItem : spanItems) {
      createSpan(builder, spanItem.getSpan(), spanItem.getSpanText());
    }
    return builder;
  }

  private static void createSpan(SpannableStringBuilder builder, Object span, String spanText) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      builder.append(spanText, span, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    } else {
      builder.append(spanText);
    }
  }
}
