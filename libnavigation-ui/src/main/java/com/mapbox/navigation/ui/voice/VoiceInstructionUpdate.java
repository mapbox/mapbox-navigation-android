package com.mapbox.navigation.ui.voice;

import androidx.core.util.Pair;

import com.mapbox.api.directions.v5.models.VoiceInstructions;

interface VoiceInstructionUpdate {

  Pair<String, String> buildTextAndTypeFrom(VoiceInstructions voiceInstructions);
}
