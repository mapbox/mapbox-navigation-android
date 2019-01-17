package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.support.annotation.NonNull;
import android.text.Spannable;
import android.text.style.ImageSpan;
import android.widget.TextView;

import com.mapbox.api.directions.v5.models.BannerComponents;
import com.mapbox.api.directions.v5.models.BannerText;
import com.squareup.picasso.Picasso;

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
class InstructionLoader {
  private TextView textView;
  private BannerComponentTree bannerComponentTree;

  InstructionLoader(TextView textView, @NonNull BannerText bannerText) {
    this(textView, new BannerComponentTree(bannerText, new ExitSignCreator(),
      new AbbreviationCreator(), ImageCreator.getInstance(), new TextCreator()));
  }

  InstructionLoader(TextView textView, BannerComponentTree bannerComponentTree) {
    this.textView = textView;
    this.bannerComponentTree = bannerComponentTree;
  }

  /**
   * Takes the given components from the {@link BannerText} and creates
   * a new {@link Spannable} with text / {@link ImageSpan}s which is loaded
   * into the given {@link TextView}.
   */
  void loadInstruction() {
    bannerComponentTree.loadInstruction(textView);
  }
}
