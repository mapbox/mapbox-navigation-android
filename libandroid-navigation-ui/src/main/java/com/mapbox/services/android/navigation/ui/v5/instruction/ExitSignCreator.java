package com.mapbox.services.android.navigation.ui.v5.instruction;

import com.mapbox.api.directions.v5.models.BannerComponents;

import java.util.List;

public class ExitSignCreator extends NodeCreator<BannerComponentNode, ExitSignVerifier> {
  private String exitText;
  private String exitNumber;

  ExitSignCreator() {
    super(new ExitSignVerifier());
  }

  @Override
  BannerComponentNode setupNode(BannerComponents components, int index, int startIndex) {
    if (components.type().equals("exit")) {
      exitText = components.text();
    } else if (components.type().equals("exit-number")) {
      exitNumber = components.text();
    }

    return null;
  }

  /**
   * One coordinator should override this method, and this should be the coordinator which populates
   * the textView with text.
   *
   * @param textView             to populate
   * @param bannerComponentNodes containing instructions
   */
  @Override
  void preProcess(InstructionTextView textView, List<BannerComponentNode> bannerComponentNodes) {
    textView.hideExitView();

    if (exitText != null && exitNumber != null) {
      textView.showExitView(exitText + " " + exitNumber);
    }
  }
}
