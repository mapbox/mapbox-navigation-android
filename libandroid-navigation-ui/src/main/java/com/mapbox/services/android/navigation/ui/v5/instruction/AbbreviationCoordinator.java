package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.graphics.Paint;
import android.widget.TextView;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class AbbreviationCoordinator {
  private static final String SINGLE_SPACE = " ";
  Map<Integer, List<Integer>> abbreviations;
  TextView textView;

  public AbbreviationCoordinator(TextView textView, Map<Integer, List<Integer>> abbreviations) {
    this.textView = textView;
    this.abbreviations = abbreviations;
  }

  public String abbreviateBannerText(List<InstructionLoader.Node> nodes) {
    String bannerText;
    int currAbbreviationPriority = 0;
    while (!textFits(textView, bannerText = join(nodes))) {
      List<Integer> indices = abbreviations.get(new Integer(currAbbreviationPriority++));

      if (indices == null) {
        continue;
      }

      for (Integer index : indices) {
        abbreviate(nodes.get(index));
      }
    }

    return bannerText;
  }

  private void abbreviate(InstructionLoader.Node node) {
    ((InstructionLoader.AbbreviationNode) node).setAbbreviate(true);
  }

  private String join(List<InstructionLoader.Node> tokens) {
    StringBuilder stringBuilder = new StringBuilder();
    Iterator<InstructionLoader.Node> iterator = tokens.iterator();
    InstructionLoader.Node node;

    if (iterator.hasNext()) {
      node = iterator.next();
      node.setStartIndex(stringBuilder.length());
      stringBuilder.append(node);

      while (iterator.hasNext()) {
        stringBuilder.append(SINGLE_SPACE);
        node = iterator.next();
        node.setStartIndex(stringBuilder.length());
        stringBuilder.append(node);
      }
    }

    return stringBuilder.toString();
  }

  private boolean textFits(TextView textView, String text) {
    Paint paint = new Paint(textView.getPaint());
    float width = paint.measureText(text);
    return width < textView.getWidth();
  }
}
