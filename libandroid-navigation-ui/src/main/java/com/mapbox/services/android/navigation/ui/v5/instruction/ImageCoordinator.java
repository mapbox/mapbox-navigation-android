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
import com.mapbox.services.android.navigation.ui.v5.instruction.InstructionLoader.BannerComponentNode;
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
 * a new {@link ImageSpan} is created and set to the appropriate position of the {@link Spannable}
 */
public class ImageCoordinator {

  private static ImageCoordinator instance;
  private boolean isInitialized;
  private Picasso picassoImageLoader;
  private List<InstructionTarget> targets;
  private UrlDensityMap urlDensityMap;
  private List<BannerShield> bannerShieldList;

  private ImageCoordinator() {
  }

  /**
   * Primary access method (using singleton pattern)
   *
   * @return ImageCoordinator
   */
  public static synchronized ImageCoordinator getInstance() {
    if (instance == null) {
      instance = new ImageCoordinator();
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
      initializePicasso(context);
      initializeData(context);
      isInitialized = true;
    }
  }

  /**
   * Uses the given BannerComponents object to construct a BannerShield object containing the
   * information needed to load the proper image into the TextView where appropriate.
   *
   * @param bannerComponents containing image info
   * @param index of the BannerComponentNode which refers to the given BannerComponents
   */
  public void addShieldInfo(BannerComponents bannerComponents, int index) {
    bannerShieldList.add(new BannerShield(bannerComponents, index));
  }

  /**
   * Will pre-fetch images for a given {@link LegStep}.
   * <p>
   * If loaded successfully, this will allow the images to be displayed
   * without delay in the {@link InstructionView}.
   *
   * @param legStep providing the image Urls
   */
  public void prefetchImageCache(LegStep legStep) {
    checkIsInitialized();
    fetchInstructions(legStep);
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
   * @since 0.9.0
   */
  public void loadImages(TextView textView, List<BannerComponentNode> bannerComponentNodes) {
    if (!hasImages()) {
      return;
    }

    updateShieldUrlIndices(bannerComponentNodes);
    createTargets(textView);
    loadTargets();
  }

  private void initializePicasso(Context context) {
    Picasso.Builder builder = new Picasso.Builder(context);
    picassoImageLoader = builder.build();
  }

  private void initializeData(Context context) {
    urlDensityMap = new UrlDensityMap(context);
    targets = new ArrayList<>();
    bannerShieldList = new ArrayList<>();
  }

  private void fetchInstructions(LegStep legStep) {
    if (legStep == null || legStep.bannerInstructions() == null
      || legStep.bannerInstructions().isEmpty()) {
      return;
    }

    List<BannerInstructions> bannerInstructionList = new ArrayList<>(legStep.bannerInstructions());
    for (BannerInstructions instructions : bannerInstructionList) {
      if (hasComponents(instructions.primary())) {
        fetchImageBaseUrls(instructions.primary());
      }
      if (hasComponents(instructions.secondary())) {
        fetchImageBaseUrls(instructions.secondary());
      }
    }
  }

  private void updateShieldUrlIndices(List<BannerComponentNode> bannerComponentNodes) {
    for (BannerShield bannerShield : bannerShieldList) {
      bannerShield.setStartIndex(bannerComponentNodes.get(bannerShield.getNodeIndex()).startIndex);
    }
  }

  private boolean hasComponents(BannerText bannerText) {
    return bannerText != null && bannerText.components() != null && !bannerText.components().isEmpty();
  }

  private boolean hasImages() {
    return !bannerShieldList.isEmpty();
  }

  /**
   * Takes a given {@link BannerText} and fetches a valid
   * imageBaseUrl if one is found.
   *
   * @param bannerText to provide the base URL
   */
  private void fetchImageBaseUrls(BannerText bannerText) {
    for (BannerComponents components : bannerText.components()) {
      if (hasImageUrl(components)) {
        picassoImageLoader.load(urlDensityMap.get(components.imageBaseUrl())).fetch();
      }
    }
  }

  private boolean hasImageUrl(BannerComponents components) {
    return !TextUtils.isEmpty(components.imageBaseUrl());
  }

  private void createTargets(TextView textView) {
    Spannable instructionSpannable = new SpannableString(textView.getText());
    for (final BannerShield bannerShield : bannerShieldList) {
      targets.add(new InstructionTarget(textView, instructionSpannable, bannerShieldList, bannerShield,
        new InstructionTarget.InstructionLoadedCallback() {
          @Override
          public void onInstructionLoaded(InstructionTarget target) {
            targets.remove(target);
          }
        }));
    }
    bannerShieldList.clear();
  }

  private void loadTargets() {
    for (InstructionTarget target : new ArrayList<>(targets)) {
      picassoImageLoader.load(urlDensityMap.get(target.getShield().getUrl()))
        .into(target);
    }
  }

  private void checkIsInitialized() {
    if (!isInitialized) {
      throw new RuntimeException("ImageCoordinator must be initialized prior to loading image URLs");
    }
  }

  static class ImageNode extends BannerComponentNode {

    ImageNode(BannerComponents bannerComponents, int startIndex) {
      super(bannerComponents, startIndex);
    }
  }
}
