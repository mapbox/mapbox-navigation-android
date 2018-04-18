package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.graphics.Paint;
import android.text.TextUtils;
import android.widget.TextView;

import com.mapbox.api.directions.v5.models.BannerComponents;

import java.util.ArrayList;
import java.util.List;

public class InstructionBuilder extends ArrayList<InstructionBuilder.Node> {
  int length = 0;
  List<BannerComponents> abbreviations;

  public InstructionBuilder(List<BannerComponents> bannerComponents, TextView textView) {
    super();
    StringBuilder instructionStringBuilder = new StringBuilder();
    List<BannerShieldInfo> shieldUrls = new ArrayList<>();
    abbreviations = new ArrayList<>();


    for (BannerComponents components : bannerComponents) {
      if (hasImageUrl(components)) {
        addShieldInfo(textView, instructionStringBuilder, shieldUrls, components);
        add(new ShieldNode(components));
      } else if (hasAbbreviation(components)) {
        instructionStringBuilder.append(components.text());
        abbreviations.add(components);
        add(new AbbreviationNode(components));
      } else {
        instructionStringBuilder.append(components.text());
        add(new Node(components));
      }

      instructionStringBuilder.append(" ");
    }
  }

  private static boolean hasAbbreviation(BannerComponents components) {
    return !TextUtils.isEmpty(components.abbreviation());
  }

  private static boolean hasImageUrl(BannerComponents components) {
    return !TextUtils.isEmpty(components.imageBaseUrl());
  }

  private static void addShieldInfo(TextView textView, StringBuilder instructionStringBuilder,
                                    List<BannerShieldInfo> shieldUrls, BannerComponents components) {
    boolean instructionBuilderEmpty = TextUtils.isEmpty(instructionStringBuilder.toString());
    int instructionLength = instructionStringBuilder.length();
    int startIndex = instructionBuilderEmpty ? instructionLength : instructionLength + 1;
    shieldUrls.add(new BannerShieldInfo(textView.getContext(), components.imageBaseUrl(),
      startIndex, components.text()));
    instructionStringBuilder.append(components.text());
  }

  public void buildInstruction(TextView textView) {
  }

  private boolean textFits(TextView textView, String text) {
    Paint paint = new Paint(textView.getPaint());
    float width = paint.measureText(text);
    return width < textView.getWidth();
  }

  class Node {
    String text;

    public Node(BannerComponents bannerComponents) {
      this.text = bannerComponents.text();
    }

    public String getText() {
      return text;
    }
  }

  class ShieldNode extends Node {
    String url;

    public ShieldNode(BannerComponents bannerComponents) {
      super(bannerComponents);

      this.url = bannerComponents.imageBaseUrl();
    }

    String getUrl() {
      return url;
    }
  }

  class AbbreviationNode extends Node {
    String abbreviation;
    int abbreviationPriority;

    public AbbreviationNode(BannerComponents bannerComponents) {
      super(bannerComponents);

      this.abbreviation = bannerComponents.abbreviation();
      this.abbreviationPriority = bannerComponents.abbreviationPriority();
    }

    String getAbbreviation() {
      return abbreviation;
    }

    int getAbbreviationPriority() {
      return abbreviationPriority;
    }
  }
}
