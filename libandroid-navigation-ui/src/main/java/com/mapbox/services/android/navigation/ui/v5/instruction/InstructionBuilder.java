package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.graphics.Paint;
import android.text.TextUtils;
import android.widget.TextView;

import com.mapbox.api.directions.v5.models.BannerComponents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class InstructionBuilder {
  int length = 0;
  Map<Integer, List<Integer>> abbreviations;
  List<BannerShieldInfo> shieldUrls;
  List<Node> nodes;
  TextView textView;

  public InstructionBuilder(List<BannerComponents> bannerComponents, TextView textView) {
    super();
    this.textView = textView;
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

  public String getBannerText() {
    String bannerText;
    int currAbbreviationPriority = 0;
    while (!textFits(textView, bannerText = join(nodes))) {
      List<Integer> indices = abbreviations.get(new Integer(currAbbreviationPriority++));

      for (Integer index : indices) {
        abbreviate(index);
      }
    }

    return bannerText;
  }

  public List<BannerShieldInfo> getShieldUrls() {
    for (BannerShieldInfo bannerShieldInfo : shieldUrls) {
      bannerShieldInfo.setStartIndex(nodes.get(bannerShieldInfo.getNodeIndex()).startIndex);
    }
    return shieldUrls;
  }

  private void abbreviate(int index) {
    ((AbbreviationNode) nodes.get(index)).setAbbreviate(true);
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

  private void addShieldInfo(TextView textView, BannerComponents components) {
    shieldUrls.add(new BannerShieldInfo(textView.getContext(), components,
      nodes.size()));
  }

  private String join(List<Node> tokens) {
    StringBuilder stringBuilder = new StringBuilder();
    Iterator<Node> iterator = tokens.iterator();
    Node node;

    if (iterator.hasNext()) {
      node = iterator.next();
      node.setStartIndex(stringBuilder.length());
      stringBuilder.append(node);

      while (iterator.hasNext()) {
        stringBuilder.append(" ");
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

  class Node {
    BannerComponents bannerComponents;
    int startIndex = -1;

    public Node(BannerComponents bannerComponents) {
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

  class ShieldNode extends Node {
    int stringIndex;


    public ShieldNode(BannerComponents bannerComponents, int stringIndex) {
      super(bannerComponents);
      this.stringIndex = stringIndex;
    }
  }

  class AbbreviationNode extends Node {
    boolean abbreviate;

    public AbbreviationNode(BannerComponents bannerComponents) {
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
