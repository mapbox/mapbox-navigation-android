package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.text.Spannable;
import android.text.style.ImageSpan;
import android.widget.TextView;

import com.mapbox.api.directions.v5.models.BannerComponents;
import com.mapbox.api.directions.v5.models.BannerText;
import com.mapbox.core.utils.TextUtils;
import com.mapbox.services.android.navigation.ui.v5.instruction.AbbreviationCoordinator.AbbreviationNode;
import com.mapbox.services.android.navigation.ui.v5.instruction.InstructionImageLoader.ShieldNode;
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
  private static InstructionImageLoader instructionImageLoader;
  private AbbreviationCoordinator abbreviationCoordinator;
  private TextView textView;
  private List<BannerComponentNode> bannerComponentNodes;

  public InstructionLoader(TextView textView, BannerText bannerText) {
    this(textView, bannerText, InstructionImageLoader.getInstance(), new AbbreviationCoordinator());
  }

  public InstructionLoader(TextView textView, BannerText bannerText, InstructionImageLoader instructionImageLoader, AbbreviationCoordinator abbreviationCoordinator) {
    this.abbreviationCoordinator = abbreviationCoordinator;
    this.textView = textView;
    bannerComponentNodes = new ArrayList<>();
    this.instructionImageLoader = instructionImageLoader;

    if (!hasComponents(bannerText)) {
      return;
    }

    bannerComponentNodes = parseBannerComponents(bannerText.components());
  }

  /**
   * Takes the given components from the {@link BannerText} and creates
   * a new {@link Spannable} with text / {@link ImageSpan}s which is loaded
   * into the given {@link TextView}.
   */
  public void loadInstruction() {
    setText(textView, bannerComponentNodes);
    loadImages(textView, bannerComponentNodes);
  }

  private List<BannerComponentNode> parseBannerComponents(List<BannerComponents> bannerComponents) {
    int length = 0;
    bannerComponentNodes = new ArrayList<>();

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

  private ShieldNode setupImageNode(BannerComponents components, TextView textView, int index, int startIndex) {
    instructionImageLoader.addShieldInfo(textView, components, index);
    return new InstructionImageLoader.ShieldNode(components, startIndex);
  }

  private AbbreviationNode setupAbbreviationNode(BannerComponents components, int index, int startIndex) {
    abbreviationCoordinator.addPriorityInfo(components, index);
    return new AbbreviationCoordinator.AbbreviationNode(components, startIndex);
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
