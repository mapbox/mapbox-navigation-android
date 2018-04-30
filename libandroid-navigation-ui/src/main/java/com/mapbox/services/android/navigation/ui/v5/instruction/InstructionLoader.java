package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.content.Context;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.widget.TextView;

import com.mapbox.api.directions.v5.models.BannerComponents;
import com.mapbox.api.directions.v5.models.BannerText;
import com.mapbox.api.directions.v5.models.LegStep;
import com.squareup.picasso.Picasso;

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
  private static InstructionLoader instance;
  private static InstructionImageLoader instructionImageLoader;
  private AbbreviationCoordinator abbreviationCoordinator;
  private List<BannerComponentNode> bannerComponentNodes;
  private List<BannerComponents> bannerComponents;
  private int length = 0;
  private boolean isInitialized;

  private InstructionLoader() {
  }

  /**
   * Primary access method (using singleton pattern)
   *
   * @return InstructionLoader
   */
  public static synchronized InstructionLoader getInstance() {
    if (instance == null) {
      instance = new InstructionLoader();
    }

    return instance;
  }

  /**
   * Must be called before loading images.
   * <p>
   * Initializes a new {@link Picasso} instance as well as the
   * {@link ArrayList} of {@link InstructionTarget}.
   *
   * @param context to init Picasso
   */
  public void initialize(Context context) {
    if (!isInitialized) {
      instructionImageLoader = InstructionImageLoader.getInstance();
      instructionImageLoader.initialize(context);

      isInitialized = true;
    }
  }

  /**
   * Will pre-fetch images for a given {@link LegStep}.
   * <p>
   * If loaded successfully, this will allow the images to be displayed
   * without delay in the {@link InstructionView}.
   *
   * @param step providing the image Urls
   */
  public void prefetchImageCache(LegStep step) {
    instructionImageLoader.prefetchImageCache(step);
  }

  public void shutdown() {
    instructionImageLoader.shutdown();
  }

  /**
   * Takes the given components from the {@link BannerText} and creates
   * a new {@link Spannable} with text / {@link ImageSpan}s which is loaded
   * into the given {@link TextView}.
   *
   * @param textView   target for the banner text
   * @param bannerText with components to be extracted
   * @since 0.9.0
   */
  public void loadInstruction(TextView textView, BannerText bannerText) {
    checkIsInitialized();

    if (!hasComponents(bannerText)) {
      return;
    }

    abbreviationCoordinator = new AbbreviationCoordinator();
    this.bannerComponents = bannerText.components();

    parseBannerComponents(textView);
    setText(textView);
    loadImages(textView);
  }

  private void loadImages(TextView textView) {
    instructionImageLoader.loadImages(textView, bannerComponentNodes);
  }

  private void setText(TextView textView) {
    String text = getAbbreviatedBannerText(textView);
    textView.setText(text);
  }

  private String getAbbreviatedBannerText(TextView textView) {
    return abbreviationCoordinator.abbreviateBannerText(bannerComponentNodes, textView);
  }

  private void parseBannerComponents(TextView textView) {
    bannerComponentNodes = new ArrayList<>();

    for (BannerComponents components : bannerComponents) {
      if (hasImageUrl(components)) {
        setupWithImages(components, textView);
      } else if (hasAbbreviation(components)) {
        setupWithAbbreviations(components);
      } else {
        bannerComponentNodes.add(new BannerComponentNode(components, length - 1));
      }
      length += components.text().length() + 1;
    }
  }

  private void setupWithImages(BannerComponents components, TextView textView) {
    instructionImageLoader.addShieldInfo(textView, components, bannerComponentNodes.size());
    bannerComponentNodes.add(new InstructionImageLoader.ShieldBannerComponentNode(components, length - 1));
  }

  private void setupWithAbbreviations(BannerComponents components) {
    abbreviationCoordinator.addPriorityInfo(components, bannerComponentNodes.size());
    bannerComponentNodes.add(new AbbreviationCoordinator.AbbreviationBannerComponentNode(components, length - 1));
  }

  private boolean hasAbbreviation(BannerComponents components) {
    return !TextUtils.isEmpty(components.abbreviation());
  }

  private boolean hasImageUrl(BannerComponents components) {
    return !TextUtils.isEmpty(components.imageBaseUrl());
  }

  private static boolean hasComponents(BannerText bannerText) {
    return bannerText != null && bannerText.components() != null && !bannerText.components().isEmpty();
  }

  private void checkIsInitialized() {
    if (!isInitialized) {
      throw new RuntimeException("InstructionLoader must be initialized prior to loading image URLs");
    }
  }

  /**
   * Class used to construct a list of BannerComponents to be populated into a TextView
   */
  static class BannerComponentNode {
    protected BannerComponents bannerComponents;
    protected int startIndex;

    protected BannerComponentNode(BannerComponents bannerComponents, int startIndex) {
      this.bannerComponents = bannerComponents;
      this.startIndex = startIndex;
    }

    @Override
    public String toString() {
      return bannerComponents.text();
    }

    public void setStartIndex(int startIndex) {
      this.startIndex = startIndex;
    }
  }
}
