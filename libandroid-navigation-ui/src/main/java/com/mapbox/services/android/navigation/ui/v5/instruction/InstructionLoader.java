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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
  List<BannerShieldInfo> shieldUrls;
  List<Node> nodes;
  Map<Integer, List<Integer>> abbreviations;
  int length = 0;

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

    parseBannerComponents(textView, bannerText.components());
    setText(textView);
    loadImages(textView);
  }

  private void loadImages(TextView textView) {
    List<BannerShieldInfo> shieldUrls = getShieldUrls();

    if (hasImages(shieldUrls)) {
      instructionImageLoader.loadImages(textView, shieldUrls);
    }
  }

  private void setText(TextView textView) {
    String text = getAbbreviatedBannerText(textView);
    textView.setText(text);
  }

  private String getAbbreviatedBannerText(TextView textView) {
    AbbreviationCoordinator abbreviationCoordinator = new AbbreviationCoordinator(textView, abbreviations);
    return abbreviationCoordinator.abbreviateBannerText(nodes);
  }

  private void parseBannerComponents(TextView textView, List<BannerComponents> bannerComponents) {
    nodes = new ArrayList<>();
    shieldUrls = new ArrayList<>();
    abbreviations = new HashMap<>();

    for (BannerComponents components : bannerComponents) {
      if (hasImageUrl(components)) {
        addShieldInfo(textView, components);
        nodes.add(new ShieldNode(components, length - 1));
        length += components.text().length();
      } else if (hasAbbreviation(components)) {
        addPriorityInfo(components);
        nodes.add(new AbbreviationNode(components));
      } else {
        nodes.add(new Node(components));
      }
      length += components.text().length() + 1;
    }
  }

  private boolean hasAbbreviation(BannerComponents components) {
    return !TextUtils.isEmpty(components.abbreviation());
  }

  private boolean hasImageUrl(BannerComponents components) {
    return !TextUtils.isEmpty(components.imageBaseUrl());
  }

  private void addPriorityInfo(BannerComponents components) {
    int abbreviationPriority = components.abbreviationPriority();
    if (abbreviations.get(Integer.valueOf(abbreviationPriority)) == null) {
      abbreviations.put(abbreviationPriority, new ArrayList<Integer>());
    }
    abbreviations.get(abbreviationPriority).add(Integer.valueOf(nodes.size()));
  }


  public List<BannerShieldInfo> getShieldUrls() {
    for (BannerShieldInfo bannerShieldInfo : shieldUrls) {
      bannerShieldInfo.setStartIndex(nodes.get(bannerShieldInfo.getNodeIndex()).startIndex);
    }
    return shieldUrls;
  }

  private void addShieldInfo(TextView textView, BannerComponents components) {
    shieldUrls.add(new BannerShieldInfo(textView.getContext(), components,
      nodes.size()));
  }

  private static boolean hasComponents(BannerText bannerText) {
    return bannerText != null && bannerText.components() != null && !bannerText.components().isEmpty();
  }

  private static boolean hasImages(List<BannerShieldInfo> shieldUrls) {
    return !shieldUrls.isEmpty();
  }

  private void checkIsInitialized() {
    if (!isInitialized) {
      throw new RuntimeException("InstructionLoader must be initialized prior to loading image URLs");
    }
  }

  static class Node {
    BannerComponents bannerComponents;
    int startIndex = -1;

    Node(BannerComponents bannerComponents) {
      this.bannerComponents = bannerComponents;
    }

    @Override
    public String toString() {
      return bannerComponents.text();
    }

    public void setStartIndex(int startIndex) {
      this.startIndex = startIndex;
    }
  }

  static class ShieldNode extends Node {
    int stringIndex;


    ShieldNode(BannerComponents bannerComponents, int stringIndex) {
      super(bannerComponents);
      this.stringIndex = stringIndex;
    }
  }

  static class AbbreviationNode extends Node {
    boolean abbreviate;

    AbbreviationNode(BannerComponents bannerComponents) {
      super(bannerComponents);
    }

    @Override
    public String toString() {
      return abbreviate ? bannerComponents.abbreviation() : bannerComponents.text();
    }

    void setAbbreviate(boolean abbreviate) {
      this.abbreviate = abbreviate;
    }
  }
}
