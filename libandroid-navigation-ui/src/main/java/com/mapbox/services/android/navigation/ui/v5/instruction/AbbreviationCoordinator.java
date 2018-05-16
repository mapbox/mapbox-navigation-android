package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.widget.TextView;

import com.mapbox.api.directions.v5.models.BannerComponents;
import com.mapbox.services.android.navigation.ui.v5.instruction.InstructionLoader.BannerComponentNode;
import com.mapbox.services.android.navigation.ui.v5.utils.TextViewUtils;

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
public class AbbreviationCoordinator {
  private static final String SINGLE_SPACE = " ";
  private Map<Integer, List<Integer>> abbreviations;
  private TextViewUtils textViewUtils;

  public AbbreviationCoordinator(TextViewUtils textViewUtils) {
    this.abbreviations = new HashMap<>();
    this.textViewUtils = textViewUtils;
  }

  public AbbreviationCoordinator() {
    this(new TextViewUtils());
  }

  /**
   * Adds the given BannerComponents object to the list of abbreviations so that when the list of
   * BannerComponentNodes is completed, text can be abbreviated properly to fit the specified
   * TextView.
   *
   * @param bannerComponents object holding the abbreviation information
   * @param index in the list of BannerComponentNodes
   */
  public void addPriorityInfo(BannerComponents bannerComponents, int index) {
    int abbreviationPriority = bannerComponents.abbreviationPriority();
    if (abbreviations.get(Integer.valueOf(abbreviationPriority)) == null) {
      abbreviations.put(abbreviationPriority, new ArrayList<Integer>());
    }
    abbreviations.get(abbreviationPriority).add(index);
  }

  /**
   * Using the abbreviations HashMap which should already be populated, abbreviates the text in the
   * bannerConmponentNodes until the text fits the given TextView.
   *
   * @param bannerComponentNodes containing the text to construct
   * @param textView to check the text fits
   * @return the properly abbreviated string that will fit in the TextView
   */
  public String abbreviateBannerText(List<BannerComponentNode> bannerComponentNodes, TextView textView) {
    String bannerText = join(bannerComponentNodes);

    if (abbreviations.isEmpty()) {
      return bannerText;
    }

    int currAbbreviationPriority = 0;
    int maxAbbreviationPriority = Collections.max(abbreviations.keySet());

    while (shouldKeepAbbreviating(textView, bannerText, currAbbreviationPriority, maxAbbreviationPriority)) {
      List<Integer> indices = abbreviations.get(new Integer(currAbbreviationPriority++));

      boolean abbreviationPriorityExists = abbreviateAtAbbreviationPriority(bannerComponentNodes, indices);

      if (abbreviationPriorityExists) {
        bannerText = join(bannerComponentNodes);
      }
    }

    abbreviations.clear();
    return bannerText;
  }

  private boolean shouldKeepAbbreviating(TextView textView, String bannerText, int currAbbreviationPriority, int maxAbbreviationPriority) {
    return !textViewUtils.textFits(textView, bannerText) && currAbbreviationPriority <= maxAbbreviationPriority;
  }

  private boolean abbreviateAtAbbreviationPriority(List<BannerComponentNode> bannerComponentNodes, List<Integer> indices) {
    if (indices == null) {
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

  /**
   * Class used by InstructionLoader to determine that a BannerComponent contains an abbreviation
   */
  static class AbbreviationNode extends BannerComponentNode {
    protected boolean abbreviate;

    AbbreviationNode(BannerComponents bannerComponents, int startIndex) {
      super(bannerComponents, startIndex);
    }

    @Override
    public String toString() {
      return abbreviate ? bannerComponents.abbreviation() : bannerComponents.text();
    }

    public void setAbbreviate(boolean abbreviate) {
      this.abbreviate = abbreviate;
    }
  }
}
