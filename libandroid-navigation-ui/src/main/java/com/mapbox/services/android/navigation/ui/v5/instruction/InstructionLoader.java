package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.IntDef;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.widget.TextView;

import com.mapbox.api.directions.v5.models.BannerComponents;
import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.api.directions.v5.models.BannerText;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class that can be used to load a given {@link BannerText} into the provided
 * {@link TextView}.
 * <p>
 * For each {@link BannerComponents}, either the text or given shield URL will be used (the shield
 * URL taking priority).
 * <p>
 * If a shield URL is found, {@link Picasso} is used to load the image.  Then, once the image is loaded,
 * a new {@link ImageSpan} is created and set to the appropriate position of the {@link Spannable}/
 */
public class InstructionLoader {

  static final int BANNER_TEXT_TYPE_PRIMARY = 0;
  static final int BANNER_TEXT_TYPE_SECONDARY = 1;
  private static final String SPACE_PLACEHOLDER = "  ";

  /**
   * Takes the given components from the {@link BannerText} and creates
   * a new {@link Spannable} with text / {@link ImageSpan}s which is loaded
   * into the given {@link TextView}.
   *
   * @param textView   target for the banner text
   * @param bannerText with components to be extracted
   * @since 0.8.0
   */
  public static void loadInstruction(TextView textView, BannerText bannerText) {

    if (hasComponents(bannerText)) {
      StringBuilder instructionStringBuilder = new StringBuilder();
      List<BannerShieldInfo> shieldUrls = new ArrayList<>();

      for (BannerComponents components : bannerText.components()) {
        if (hasBaseUrl(components)) {
          addShieldInfo(textView, instructionStringBuilder, shieldUrls, components);
        } else {
          instructionStringBuilder.append(bannerText.text());
        }
      }

      // If there are shield Urls, fetch the corresponding images
      if (!shieldUrls.isEmpty()) {
        fetchShieldImages(textView, instructionStringBuilder, shieldUrls);
      } else {
        textView.setText(instructionStringBuilder);
      }
    }
  }

  static BannerText findInstructionBannerText(double stepDistanceRemaining,
                                              List<BannerInstructions> instructions,
                                              @BannerTextType int bannerTextType) {

    if (instructions == null) {
      return null;
    }

    boolean returnPrimary = bannerTextType == BANNER_TEXT_TYPE_PRIMARY;

    if (instructions.size() == 1 || stepDistanceRemaining == 0) {
      return returnPrimary ? instructions.get(0).primary() : instructions.get(0).secondary();
    }

    for (int i = 0; i < instructions.size(); i++) {

      boolean validCurrentInstruction = instructions.get(i).distanceAlongGeometry() >= stepDistanceRemaining;
      boolean nextInstructionNull = instructions.get(i + 1) == null;

      if (validCurrentInstruction && nextInstructionNull) {
        return returnPrimary ? instructions.get(0).primary() : instructions.get(0).secondary();
      }

      if (validCurrentInstruction && instructions.get(i + 1).distanceAlongGeometry() <= stepDistanceRemaining) {
        return returnPrimary ? instructions.get(0).primary() : instructions.get(0).secondary();
      }
    }
    return null;
  }

  private static boolean hasComponents(BannerText bannerText) {
    return bannerText.components() != null && !bannerText.components().isEmpty();
  }

  private static boolean hasBaseUrl(BannerComponents components) {
    return !TextUtils.isEmpty(components.imageBaseUrl());
  }

  private static void addShieldInfo(TextView textView, StringBuilder instructionStringBuilder,
                                    List<BannerShieldInfo> shieldUrls, BannerComponents components) {
    shieldUrls.add(new BannerShieldInfo(textView.getContext(), components.imageBaseUrl(),
      instructionStringBuilder.length(), components.text()));
    instructionStringBuilder.append(SPACE_PLACEHOLDER);
  }

  private static void fetchShieldImages(final TextView textView, StringBuilder instructionStringBuilder,
                                        final List<BannerShieldInfo> shields) {

    final Spannable instructionSpannable = new SpannableString(instructionStringBuilder);
    final Context context = textView.getContext();

    // Use Picasso to load a Drawable from the given url
    for (final BannerShieldInfo shield : shields) {
      Picasso.with(context)
        .load(shield.getUrl())
        .into(new Target() {
          @Override
          public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            // Create a new Drawable with intrinsic bounds
            Drawable drawable = new BitmapDrawable(context.getResources(), bitmap);
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());

            // Create and set a new ImageSpan at the given index with the Drawable
            instructionSpannable.setSpan(new ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM),
              shield.getStartIndex(), shield.getEndIndex(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Check if last in array, if so, set the text with the spannable
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

  @Retention(RetentionPolicy.SOURCE)
  @IntDef( {
    BANNER_TEXT_TYPE_PRIMARY,
    BANNER_TEXT_TYPE_SECONDARY
  })
  @interface BannerTextType {
  }
}
