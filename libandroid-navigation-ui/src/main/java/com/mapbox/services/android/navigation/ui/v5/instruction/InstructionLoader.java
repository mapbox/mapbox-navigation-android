package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.widget.TextView;

import com.mapbox.api.directions.v5.models.BannerComponents;
import com.mapbox.api.directions.v5.models.BannerInstructions;
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
  private boolean isInitialized;
  private Picasso picassoImageLoader;
  private List<InstructionTarget> targets;
  private UrlDensityMap urlDensityMap;
  private static final String IMAGE_SPACE_PLACEHOLDER = "  ";
  private static final String SINGLE_SPACE = " ";

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
      Picasso.Builder builder = new Picasso.Builder(context)
        .loggingEnabled(true);
      picassoImageLoader = builder.build();

      this.urlDensityMap = new UrlDensityMap(context);
      targets = new ArrayList<>();

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

    if (step == null || step.bannerInstructions() == null
      || step.bannerInstructions().isEmpty()) {
      return;
    }

    checkIsInitialized();

    List<BannerInstructions> bannerInstructionList = new ArrayList<>(step.bannerInstructions());
    for (BannerInstructions instructions : bannerInstructionList) {
      if (hasComponents(instructions.primary())) {
        fetchImageBaseUrls(instructions.primary());
      }
      if (hasComponents(instructions.secondary())) {
        fetchImageBaseUrls(instructions.secondary());
      }
    }
  }

  public void shutdown() {
    targets.clear();
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

    if (hasComponents(bannerText)) {
      StringBuilder instructionStringBuilder = new StringBuilder();
      List<BannerShieldInfo> shieldUrls = new ArrayList<>();

      for (BannerComponents components : bannerText.components()) {
        if (hasBaseUrl(components)) {
          addShieldInfo(textView, instructionStringBuilder, shieldUrls, components);
        } else {
          String text = components.text();
          boolean emptyText = TextUtils.isEmpty(instructionStringBuilder.toString());
          String instructionText = emptyText ? text : SINGLE_SPACE.concat(text);
          instructionStringBuilder.append(instructionText);
        }
      }

      // If there are shield Urls, fetch the corresponding images
      if (!shieldUrls.isEmpty()) {
        createTargets(textView, instructionStringBuilder, shieldUrls);
        loadTargets();
      } else {
        textView.setText(instructionStringBuilder);
      }
    }
  }

  private static boolean hasComponents(BannerText bannerText) {
    return bannerText != null && bannerText.components() != null && !bannerText.components().isEmpty();
  }

  /**
   * Takes a given {@link BannerText} and fetches a valid
   * imageBaseUrl if one is found.
   *
   * @param bannerText to provide the base URL
   */
  private void fetchImageBaseUrls(BannerText bannerText) {
    for (BannerComponents components : bannerText.components()) {
      if (hasBaseUrl(components)) {
        picassoImageLoader.load(urlDensityMap.get(components.imageBaseUrl())).fetch();
      }
    }
  }

  private static boolean hasBaseUrl(BannerComponents components) {
    return !TextUtils.isEmpty(components.imageBaseUrl());
  }

  private static void addShieldInfo(TextView textView, StringBuilder instructionStringBuilder,
                                    List<BannerShieldInfo> shieldUrls, BannerComponents components) {
    boolean instructionBuilderEmpty = TextUtils.isEmpty(instructionStringBuilder.toString());
    int instructionLength = instructionStringBuilder.length();
    int startIndex = instructionBuilderEmpty ? instructionLength : instructionLength + 1;
    shieldUrls.add(new BannerShieldInfo(textView.getContext(), components.imageBaseUrl(),
      startIndex, components.text()));
    instructionStringBuilder.append(IMAGE_SPACE_PLACEHOLDER);
  }

  private void createTargets(TextView textView, StringBuilder instructionStringBuilder,
                             List<BannerShieldInfo> shields) {
    Spannable instructionSpannable = new SpannableString(instructionStringBuilder);
    for (final BannerShieldInfo shield : shields) {
      targets.add(new InstructionTarget(textView, instructionSpannable, shields, shield,
        new InstructionTarget.InstructionLoadedCallback() {
          @Override
          public void onInstructionLoaded(InstructionTarget target) {
            targets.remove(target);
          }
        }));
    }
  }

  private void loadTargets() {
    for (InstructionTarget target : new ArrayList<>(targets)) {
      picassoImageLoader.load(target.getShield().getUrl())
        .into(target);
    }
  }

  private void checkIsInitialized() {
    if (!isInitialized) {
      throw new RuntimeException("InstructionLoader must be initialized prior to loading image URLs");
    }
  }
}
