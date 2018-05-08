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
    initialize(context, new AbbreviationCoordinator());
  }

  public void initialize(Context context, AbbreviationCoordinator abbreviationCoordinator) {
    if (!isInitialized) {
      instructionImageLoader = InstructionImageLoader.getInstance();
      instructionImageLoader.initialize(context);
      this.abbreviationCoordinator = abbreviationCoordinator;

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

    loadAbbreviationsAndImages(textView, bannerText.components());
  }

  private void loadAbbreviationsAndImages(TextView textView, List<BannerComponents> bannerComponents) {
    List<BannerComponentNode> bannerComponentNodes = parseBannerComponents(textView, bannerComponents);
    setText(textView, bannerComponentNodes);
    loadImages(textView, bannerComponentNodes);
  }

  private List<BannerComponentNode> parseBannerComponents(TextView textView, List<BannerComponents> bannerComponents) {
    int length = 0;
    List<BannerComponentNode> bannerComponentNodes = new ArrayList<>();

    for (BannerComponents components : bannerComponents) {
      BannerComponentNode node;
      if (hasImageUrl(components)) {
        node = setupImageNode(components, textView, bannerComponentNodes.size(), length - 1);
      } else if (hasAbbreviation(components)) {
        node = setupAbbreviationNode(components, bannerComponentNodes.size(), length - 1);
      } else {
        node = new BannerComponentNode(components, length - 1);
      }
      bannerComponentNodes.add(node);
      length += components.text().length() + 1;
    }

    return bannerComponentNodes;
  }

  private InstructionImageLoader.ShieldBannerComponentNode setupImageNode(BannerComponents components, TextView textView, int index, int startIndex) {
    instructionImageLoader.addShieldInfo(textView, components, index);
    return new InstructionImageLoader.ShieldBannerComponentNode(components, startIndex);
  }

  private AbbreviationCoordinator.AbbreviationBannerComponentNode setupAbbreviationNode(BannerComponents components, int index, int startIndex) {
    abbreviationCoordinator.addPriorityInfo(components, index);
    return new AbbreviationCoordinator.AbbreviationBannerComponentNode(components, startIndex);
  }

  private void loadImages(TextView textView, List<BannerComponentNode> bannerComponentNodes) {
    instructionImageLoader.loadImages(textView, bannerComponentNodes);
  }

  private void setText(TextView textView, List<BannerComponentNode> bannerComponentNodes) {
    String text = getAbbreviatedBannerText(textView, bannerComponentNodes);
    textView.setText(text);
  }

  private String getAbbreviatedBannerText(TextView textView, List<BannerComponentNode> bannerComponentNodes) {
    return abbreviationCoordinator.abbreviateBannerText(bannerComponentNodes, textView);
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
