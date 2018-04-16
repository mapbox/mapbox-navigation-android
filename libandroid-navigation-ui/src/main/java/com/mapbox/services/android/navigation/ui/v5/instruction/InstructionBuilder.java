package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.text.TextUtils;
import android.widget.TextView;

import com.mapbox.api.directions.v5.models.BannerComponents;

import java.util.ArrayList;
import java.util.List;

public class InstructionBuilder extends ArrayList<InstructionBuilder.Node> {
  int length = 0;

  public InstructionBuilder(List<BannerComponents> bannerComponents) {
    super();
    StringBuilder instructionStringBuilder = new StringBuilder();
    List<BannerShieldInfo> shieldUrls = new ArrayList<>();


    for (BannerComponents components : bannerComponents) {
      // todo: create StringBuilder and add nodes to list
      if (hasImageUrl(components)) {
        addShieldInfo(textView, instructionStringBuilder, shieldUrls, components);

      } else {
//        String text = components.text();
//        boolean textViewIsEmpty = TextUtils.isEmpty(instructionStringBuilder.toString());
//        String instructionText = textViewIsEmpty ? text : SINGLE_SPACE.concat(text);
        instructionStringBuilder.append(components.text());
      }
    }
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

  class Node {
    int index;
    String text;
  }

  class ShieldNode extends Node {
    String getUrl() {
      return "";
    }

    String getText() {
      return "";
    }
  }

  class AbbreviationNode extends Node {
    String getAbbreviation() {
      return "";
    }

    int getAbbreviationPriority() {
      return -1;
    }
  }
}
