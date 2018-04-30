package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.graphics.Paint;
import android.widget.TextView;

import com.mapbox.api.directions.v5.models.BannerComponents;
import com.mapbox.services.android.navigation.ui.v5.instruction.InstructionLoader.BannerComponentNode;

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

  public AbbreviationCoordinator() {
    abbreviations = new HashMap<>();
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
    while (!textFits(textView, bannerText) && (currAbbreviationPriority > maxAbbreviationPriority)) {
      List<Integer> indices = abbreviations.get(new Integer(currAbbreviationPriority++));

      if (indices == null) {
        continue;
      }

      for (Integer index : indices) {
        abbreviate(bannerComponentNodes.get(index));
      }

      bannerText = join(bannerComponentNodes);
    }

    return bannerText;
  }

  private void abbreviate(BannerComponentNode bannerComponentNode) {
    ((AbbreviationBannerComponentNode) bannerComponentNode).setAbbreviate(true);
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

  private boolean textFits(TextView textView, String text) {
    Paint paint = new Paint(textView.getPaint());
    float width = paint.measureText(text);
    return width < textView.getWidth();
  }

  /**
   * Class used by InstructionLoader to determine that a BannerComponent contains an abbreviation
   */
  static class AbbreviationBannerComponentNode extends BannerComponentNode {
    protected boolean abbreviate;

    AbbreviationBannerComponentNode(BannerComponents bannerComponents, int startIndex) {
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
