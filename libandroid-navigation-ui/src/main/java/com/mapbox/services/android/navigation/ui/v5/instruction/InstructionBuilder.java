package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.widget.TextView;

import com.mapbox.api.directions.v5.models.BannerComponents;

import java.util.ArrayList;
import java.util.List;

public class InstructionBuilder extends ArrayList<InstructionBuilder.Node> {

  public InstructionBuilder(List<BannerComponents> bannerComponents) {

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
