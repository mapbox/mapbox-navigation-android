package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.widget.TextView;

import com.mapbox.api.directions.v5.models.BannerComponents;
import com.mapbox.api.directions.v5.models.BannerText;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;
import java.util.List;

public class InstructionLoader {

  public static void loadInstruction(TextView textView, BannerText bannerText) {
    if (bannerText.components() != null && !bannerText.components().isEmpty()) {

      StringBuilder instructionStringBuilder = new StringBuilder();
      List<BannerShield> shieldUrls = new ArrayList<>();

      for (BannerComponents components : bannerText.components()) {
        if (!TextUtils.isEmpty(components.imageBaseUrl())) {
          shieldUrls.add(new BannerShield(components.imageBaseUrl(), instructionStringBuilder.length(), components.text()));
          instructionStringBuilder.append("  ");
        } else {
          instructionStringBuilder.append(bannerText.text());
        }
      }

      if (!shieldUrls.isEmpty()) {
        fetchShieldImages(textView, instructionStringBuilder, shieldUrls);
      }
    }
  }

  private static void fetchShieldImages(final TextView textView, StringBuilder instructionStringBuilder, final List<BannerShield> shields) {
    final Spannable instructionSpannable = new SpannableString(instructionStringBuilder);
    for (final BannerShield shield : shields) {
      final Context context = textView.getContext();
      Picasso.with(context)
        .load(shield.getUrl())
        .into(new Target() {
          @Override
          public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            Drawable drawable = new BitmapDrawable(context.getResources(), bitmap);
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            instructionSpannable.setSpan(new ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM), shield.getStartIndex(), shield.getEndIndex(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Check if last in array, if so, set the text
            if (shields.indexOf(shield) == shields.size() - 1) {
              textView.setText(instructionSpannable);
            }
          }

          @Override
          public void onBitmapFailed(Drawable errorDrawable) {

          }

          @Override
          public void onPrepareLoad(Drawable placeHolderDrawable) {

          }
        });
    }
  }

  private static class BannerShield {
    private String url;
    private int startIndex;

    BannerShield(String url, int startIndex, String textReplacement) {
      this.url = url + "@3x.png";
      this.startIndex = startIndex;
    }

    String getUrl() {
      return url;
    }

    int getStartIndex() {
      return startIndex;
    }

    int getEndIndex() {
      return startIndex + 1;
    }
  }
}
