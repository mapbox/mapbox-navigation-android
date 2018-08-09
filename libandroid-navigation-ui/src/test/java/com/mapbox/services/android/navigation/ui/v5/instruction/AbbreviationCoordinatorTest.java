package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.widget.TextView;

import com.mapbox.api.directions.v5.models.BannerComponents;
import com.mapbox.services.android.navigation.ui.v5.BaseTest;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbbreviationCoordinatorTest extends BaseTest {
  @Test
  public void onAbbreviateBannerText_textIsAbbreviated() {
    String abbreviation = "smtxt";
    BannerComponents bannerComponents =
      BannerComponentsFaker.bannerComponents()
        .abbreviation(abbreviation)
        .abbreviationPriority(0)
        .build();
    TextViewUtils textViewUtils = mock(TextViewUtils.class);
    TextView textView = mock(TextView.class);
    AbbreviationVerifier abbreviationVerifier = mock(AbbreviationVerifier.class);
    when(abbreviationVerifier.isNodeType(bannerComponents)).thenReturn(true);
    when(textViewUtils.textFits(textView, abbreviation)).thenReturn(true);
    when(textViewUtils.textFits(textView, bannerComponents.text())).thenReturn(false);
    AbbreviationCreator abbreviationCoordinator = new AbbreviationCreator(textViewUtils, abbreviationVerifier);
    abbreviationCoordinator.addPriorityInfo(bannerComponents, 0);
    List<BannerComponentNode> bannerComponentNodes = new ArrayList<>();
    bannerComponentNodes.add(new AbbreviationCreator.AbbreviationNode(bannerComponents, 0));

    String abbreviatedTextFromCoordinator = abbreviationCoordinator.abbreviateBannerText(textView, bannerComponentNodes);

    assertEquals(abbreviation, abbreviatedTextFromCoordinator);
  }

  @Test
  public void onAbbreviateBannerText_textIsNotAbbreviated() {
    String abbreviation = "smtxt";
    String text = "some text";
    BannerComponents bannerComponents =
      BannerComponentsFaker.bannerComponents()
        .abbreviation(abbreviation)
        .abbreviationPriority(0)
        .text(text)
        .build();
    TextViewUtils textViewUtils = mock(TextViewUtils.class);
    TextView textView = mock(TextView.class);
    AbbreviationVerifier abbreviationVerifier = mock(AbbreviationVerifier.class);
    when(abbreviationVerifier.isNodeType(bannerComponents)).thenReturn(true);
    when(textViewUtils.textFits(textView, bannerComponents.text())).thenReturn(true);
    AbbreviationCreator abbreviationCoordinator = new AbbreviationCreator(textViewUtils, abbreviationVerifier);
    abbreviationCoordinator.addPriorityInfo(bannerComponents, 0);
    List<BannerComponentNode> bannerComponentNodes = new ArrayList<>();
    bannerComponentNodes.add(new AbbreviationCreator.AbbreviationNode(bannerComponents, 0));

    String abbreviatedTextFromCoordinator = abbreviationCoordinator.abbreviateBannerText(textView, bannerComponentNodes);

    assertEquals(text, abbreviatedTextFromCoordinator);
  }
}
