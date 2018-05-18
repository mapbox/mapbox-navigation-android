package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.support.annotation.NonNull;
import android.text.Spannable;
import android.text.style.ImageSpan;
import android.widget.TextView;

import com.mapbox.api.directions.v5.models.BannerComponents;
import com.mapbox.api.directions.v5.models.BannerText;
import com.mapbox.core.utils.TextUtils;
import com.mapbox.services.android.navigation.ui.v5.instruction.AbbreviationCoordinator.AbbreviationNode;
import com.mapbox.services.android.navigation.ui.v5.instruction.ImageCoordinator.ImageNode;
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
class InstructionLoader {
  private ImageCoordinator imageCoordinator;
  private AbbreviationCoordinator abbreviationCoordinator;
  private TextView textView;
  private List<BannerComponentNode> bannerComponentNodes;

  InstructionLoader(TextView textView, @NonNull List<BannerComponents> bannerComponents) {
    this(textView, bannerComponents, ImageCoordinator.getInstance(), new AbbreviationCoordinator());
  }

  InstructionLoader(TextView textView, @NonNull List<BannerComponents> bannerComponents,
                    ImageCoordinator imageCoordinator, AbbreviationCoordinator abbreviationCoordinator) {
    this.abbreviationCoordinator = abbreviationCoordinator;
    this.textView = textView;
    bannerComponentNodes = new ArrayList<>();
    this.imageCoordinator = imageCoordinator;

    bannerComponentNodes = parseBannerComponents(bannerComponents);
  }

  /**
   * Takes the given components from the {@link BannerText} and creates
   * a new {@link Spannable} with text / {@link ImageSpan}s which is loaded
   * into the given {@link TextView}.
   */
  void loadInstruction() {
    setText(textView, bannerComponentNodes);
    loadImages(textView, bannerComponentNodes);
  }

  private List<BannerComponentNode> parseBannerComponents(List<BannerComponents> bannerComponents) {
    int length = 0;
    bannerComponentNodes = new ArrayList<>();

    for (BannerComponents components : bannerComponents) {
      BannerComponentNode node;
      if (hasImageUrl(components)) {
        node = setupImageNode(components, bannerComponentNodes.size(), length - 1);
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

  private ImageNode setupImageNode(BannerComponents components, int index, int startIndex) {
    imageCoordinator.addShieldInfo(components, index);
    return new ImageNode(components, startIndex);
  }

  private AbbreviationNode setupAbbreviationNode(BannerComponents components, int index, int startIndex) {
    abbreviationCoordinator.addPriorityInfo(components, index);
    return new AbbreviationCoordinator.AbbreviationNode(components, startIndex);
  }

  private void loadImages(TextView textView, List<BannerComponentNode> bannerComponentNodes) {
    imageCoordinator.loadImages(textView, bannerComponentNodes);
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

  /**
   * Class used to construct a list of BannerComponents to be populated into a TextView
   */
  static class BannerComponentNode {
    BannerComponents bannerComponents;
    int startIndex;

    BannerComponentNode(BannerComponents bannerComponents, int startIndex) {
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
