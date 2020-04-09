package com.mapbox.navigation.ui.voice;


import androidx.core.util.Pair;

import com.mapbox.api.directions.v5.models.VoiceInstructions;

import java.util.HashMap;

class VoiceInstructionMap extends HashMap<Boolean, VoiceInstructionUpdate> {

  private static final String SSML_TEXT_TYPE = "ssml";
  private static final String TEXT_TYPE = "text";

  VoiceInstructionMap() {
    super(2);
    put(true, new VoiceInstructionUpdate() {
      @Override
      public Pair<String, String> buildTextAndTypeFrom(VoiceInstructions voiceInstructions) {
        return new Pair<>(voiceInstructions.ssmlAnnouncement(), SSML_TEXT_TYPE);
      }
    });
    put(false, new VoiceInstructionUpdate() {
      @Override
      public Pair<String, String> buildTextAndTypeFrom(VoiceInstructions voiceInstructions) {
        return new Pair<>(voiceInstructions.announcement(), TEXT_TYPE);
      }
    });
  }
}
