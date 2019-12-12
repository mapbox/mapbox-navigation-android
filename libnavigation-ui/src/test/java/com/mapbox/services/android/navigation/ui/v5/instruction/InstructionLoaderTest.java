package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.widget.TextView;

import com.mapbox.navigation.ui.v5.instruction.BannerComponentTree;
import com.mapbox.navigation.ui.instruction.InstructionLoader;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class InstructionLoaderTest {

  @Test
  public void loadInstruction() {
    TextView textView = mock(TextView.class);
    BannerComponentTree bannerComponentTree = mock(BannerComponentTree.class);
    InstructionLoader instructionLoader = new InstructionLoader(textView, bannerComponentTree);

    instructionLoader.loadInstruction();

    verify(bannerComponentTree).loadInstruction(textView);
  }
}