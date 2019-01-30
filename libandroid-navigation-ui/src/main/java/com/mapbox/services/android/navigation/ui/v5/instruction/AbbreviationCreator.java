package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.widget.TextView;

import com.mapbox.api.directions.v5.models.BannerComponents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This class allows text to be constructed to fit a given TextView, given specified
 * BannerComponents containing abbreviation information and given a list of BannerComponentNodes,
 * constructed by InstructionLoader.
 */
class AbbreviationCreator extends NodeCreator<AbbreviationCreator.AbbreviationNode, AbbreviationVerifier> {
  private static final String SINGLE_SPACE = " ";
  private Map<Integer, List<Integer>> abbreviations;
  private TextViewUtils textViewUtils;

  AbbreviationCreator(AbbreviationVerifier abbreviationVerifier, HashMap abbreviations,
                      TextViewUtils textViewUtils) {
    super(abbreviationVerifier);
    this.abbreviations = abbreviations;
    this.textViewUtils = textViewUtils;
  }

  AbbreviationCreator(AbbreviationVerifier abbreviationVerifier) {
    this(abbreviationVerifier, new HashMap(), new TextViewUtils());
  }

  AbbreviationCreator() {
    this(new AbbreviationVerifier());
  }

  /**
   * Adds the given BannerComponents object to the list of abbreviations so that when the list of
   * BannerComponentNodes is completed, text can be abbreviated properly to fit the specified
   * TextView.
   *
   * @param bannerComponents object holding the abbreviation information
   * @param index in the list of BannerComponentNodes
   */
  private void addPriorityInfo(BannerComponents bannerComponents, int index) {
    Integer abbreviationPriority = bannerComponents.abbreviationPriority();
    if (abbreviations.get(abbreviationPriority) == null) {
      abbreviations.put(abbreviationPriority, new ArrayList<Integer>());
    }
    abbreviations.get(abbreviationPriority).add(index);
  }

  /**
   * Using the abbreviations HashMap which should already be populated, abbreviates the text in the
   * bannerComponentNodes until the text fits the given TextView.
   *
   * @param textView to check the text fits
   * @param bannerComponentNodes containing the text to construct
   * @return the properly abbreviated string that will fit in the TextView
   */
  private String abbreviateBannerText(TextView textView, List<BannerComponentNode>
    bannerComponentNodes) {
    String bannerText = join(bannerComponentNodes);

    if (abbreviations.isEmpty()) {
      return bannerText;
    }

    bannerText = abbreviateUntilTextFits(textView, bannerText, bannerComponentNodes);

    abbreviations.clear();
    return bannerText;
  }

  private String abbreviateUntilTextFits(TextView textView, String startingText,
                                         List<BannerComponentNode> bannerComponentNodes) {
    int currAbbreviationPriority = 0;
    int maxAbbreviationPriority = Collections.max(abbreviations.keySet());
    String bannerText = startingText;

    while (shouldKeepAbbreviating(textView, bannerText, currAbbreviationPriority, maxAbbreviationPriority)) {
      List<Integer> indices = abbreviations.get(currAbbreviationPriority++);

      boolean abbreviationPriorityExists = abbreviateAtAbbreviationPriority(bannerComponentNodes, indices);

      if (abbreviationPriorityExists) {
        bannerText = join(bannerComponentNodes);
      }
    }

    return bannerText;
  }

  private boolean shouldKeepAbbreviating(TextView textView, String bannerText,
                                         int currAbbreviationPriority, int maxAbbreviationPriority) {

    boolean textFits = textViewUtils.textFits(textView, bannerText);
    boolean abbreviationPrioritiesLeft = currAbbreviationPriority <= maxAbbreviationPriority;
    return !textFits && abbreviationPrioritiesLeft;
  }

  private boolean abbreviateAtAbbreviationPriority(List<BannerComponentNode> bannerComponentNodes,
                                                   List<Integer> indices) {
    if (indices == null || indices.isEmpty()) {
      return false;
    }

    for (Integer index : indices) {
      abbreviate(bannerComponentNodes.get(index));
    }

    return true;
  }

  private void abbreviate(BannerComponentNode bannerComponentNode) {
    ((AbbreviationNode) bannerComponentNode).setAbbreviate(true);
  }

  private String join(List<BannerComponentNode> tokens) {
    StringBuilder stringBuilder = new StringBuilder();
    Iterator<BannerComponentNode> iterator = tokens.iterator();
    BannerComponentNode bannerComponentNode;

    if (iterator.hasNext()) {
      bannerComponentNode = iterator.next();
      bannerComponentNode.setStartIndex(stringBuilder.length());
      stringBuilder.append(bannerComponentNode);

      while (iterator.hasNext()) {
        stringBuilder.append(SINGLE_SPACE);
        bannerComponentNode = iterator.next();
        bannerComponentNode.setStartIndex(stringBuilder.length());
        stringBuilder.append(bannerComponentNode);
      }
    }

    return stringBuilder.toString();
  }

  @Override
  AbbreviationNode setupNode(BannerComponents components, int index, int startIndex, String
    modifier) {
    addPriorityInfo(components, index);
    return new AbbreviationCreator.AbbreviationNode(components, startIndex);
  }

  @Override
  void preProcess(TextView textView, List<BannerComponentNode> bannerComponentNodes) {
    String text = abbreviateBannerText(textView, bannerComponentNodes);
    textView.setText(text);
  }

  /**
   * Class used by InstructionLoader to determine that a BannerComponent contains an abbreviation
   */
  static class AbbreviationNode extends BannerComponentNode {
    private boolean abbreviate;

    AbbreviationNode(BannerComponents bannerComponents, int startIndex) {
      super(bannerComponents, startIndex);
    }

    @Override
    public String toString() {
      return abbreviate ? bannerComponents.abbreviation() : bannerComponents.text();
    }

    void setAbbreviate(boolean abbreviate) {
      this.abbreviate = abbreviate;
    }

    boolean getAbbreviate() {
      return abbreviate;
    }
  }
}
