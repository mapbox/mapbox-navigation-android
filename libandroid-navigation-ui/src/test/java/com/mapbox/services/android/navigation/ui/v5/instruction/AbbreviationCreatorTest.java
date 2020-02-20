package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.widget.TextView;

import com.mapbox.api.directions.v5.models.BannerComponents;
import com.mapbox.services.android.navigation.ui.v5.BaseTest;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AbbreviationCreatorTest extends BaseTest {

  @Test
  public void preProcess_abbreviate() {
    String abbreviation = "smtxt";
    BannerComponents bannerComponents =
      BannerComponentsFaker.bannerComponentsBuilder()
        .abbreviation(abbreviation)
        .abbreviationPriority(0)
        .build();
    TextView textView = mock(TextView.class);
    AbbreviationVerifier abbreviationVerifier = mock(AbbreviationVerifier.class);
    when(abbreviationVerifier.isNodeType(bannerComponents)).thenReturn(true);
    TextViewUtils textViewUtils = mock(TextViewUtils.class);
    when(textViewUtils.textFits(textView, abbreviation)).thenReturn(true);
    when(textViewUtils.textFits(textView, bannerComponents.text())).thenReturn(false);
    BannerComponentNode node = mock(AbbreviationCreator.AbbreviationNode.class);
    when(((AbbreviationCreator.AbbreviationNode) node).getAbbreviate()).thenReturn(true);
    when(node.toString()).thenReturn(abbreviation);
    AbbreviationCreator abbreviationCreator = new AbbreviationCreator(abbreviationVerifier);

    abbreviationCreator.preProcess(textView, Collections.singletonList(node));

    verify(textView).setText(abbreviation);
  }

  @Test
  public void setupNode() {
    String abbreviation = "smtxt";
    int abbreviationPriority = 0;
    BannerComponents bannerComponents =
      BannerComponentsFaker.bannerComponentsBuilder()
        .abbreviation(abbreviation)
        .abbreviationPriority(abbreviationPriority)
        .build();
    AbbreviationVerifier abbreviationVerifier = mock(AbbreviationVerifier.class);
    when(abbreviationVerifier.isNodeType(bannerComponents)).thenReturn(true);
    HashMap<Integer, List<Integer>> abbreviations = new HashMap();
    AbbreviationCreator abbreviationCreator = new AbbreviationCreator(abbreviationVerifier,
      abbreviations, mock(TextViewUtils.class));
    List<BannerComponentNode> bannerComponentNodes = new ArrayList<>();
    bannerComponentNodes.add(new AbbreviationCreator.AbbreviationNode(bannerComponents, 0));

    abbreviationCreator.setupNode(bannerComponents, 0, 0, "");

    assertEquals(abbreviations.size(), 1);
    assertEquals(abbreviations.get(abbreviationPriority).get(0), Integer.valueOf(0));
  }
}
