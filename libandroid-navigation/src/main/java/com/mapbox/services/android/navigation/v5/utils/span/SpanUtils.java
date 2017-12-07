package com.mapbox.services.android.navigation.v5.utils.span;

import android.graphics.Typeface;
import android.os.Build;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.StyleSpan;

import com.mapbox.api.directions.v5.models.BannerComponents;
import com.mapbox.api.directions.v5.models.BannerText;

import java.util.ArrayList;
import java.util.List;

public class SpanUtils {

  public static SpannableStringBuilder combineSpans(List<SpanItem> spanItems) {
    SpannableStringBuilder builder = new SpannableStringBuilder();
    for (SpanItem item : spanItems) {
      if (item instanceof TextSpanItem) {
        appendTextSpan(builder, item.getSpan(), ((TextSpanItem) item).getSpanText());
      }
      if (item instanceof ImageSpanItem) {
        appendImageSpan(builder, item);
      }
    }
    return builder;
  }

  public static SpannableStringBuilder buildInstructionSpanItems(BannerText bannerText) {
    List<SpanItem> spanItems = new ArrayList<>();
    if (hasComponents(bannerText)) {
      for (BannerComponents components : bannerText.components()) {
        if (!TextUtils.isEmpty(components.imageBaseUrl())) {
          spanItems.add(new ImageSpanItem(components.imageBaseUrl()));
        } else {
          spanItems.add(new TextSpanItem(new StyleSpan(Typeface.BOLD), components.text()));
        }
      }
    }
    return combineSpans(spanItems);
  }

  private static boolean hasComponents(BannerText text) {
    return text.components() != null && !text.components().isEmpty();
  }

  private static void appendTextSpan(SpannableStringBuilder builder, Object span, String spanText) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      builder.append(spanText, span, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    } else {
      builder.append(spanText);
    }
  }

  private static void appendImageSpan(SpannableStringBuilder builder, SpanItem item) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      builder.append("Image text", item.getSpan(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
  }
}
