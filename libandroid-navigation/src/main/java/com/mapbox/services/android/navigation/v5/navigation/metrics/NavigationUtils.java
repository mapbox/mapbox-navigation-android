package com.mapbox.services.android.navigation.v5.navigation.metrics;

import android.content.Context;
import android.media.AudioManager;
import android.provider.Settings;

import com.mapbox.services.android.navigation.v5.navigation.metrics.audio.AudioTypeChain;
import com.mapbox.services.android.navigation.v5.navigation.metrics.audio.AudioTypeResolver;

class NavigationUtils {
  private static final double PERCENT_NORMALIZER = 100.0;
  private static final double SCREEN_BRIGHTNESS_MAX = 255.0;
  private static final int BRIGHTNESS_EXCEPTION_VALUE = -1;

  static int obtainVolumeLevel(Context context) {
    AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    return (int) Math.floor(PERCENT_NORMALIZER * audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
      / audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
  }

  static int obtainScreenBrightness(Context context) {
    int screenBrightness;
    try {
      screenBrightness = Settings.System.getInt(context.getContentResolver(),
        Settings.System.SCREEN_BRIGHTNESS);

      screenBrightness = calculateScreenBrightnessPercentage(screenBrightness);
    } catch (Settings.SettingNotFoundException exception) {
      screenBrightness = BRIGHTNESS_EXCEPTION_VALUE;
    }
    return screenBrightness;
  }

  static String obtainAudioType(Context context) {
    AudioTypeChain audioTypeChain = new AudioTypeChain();
    AudioTypeResolver setupChain = audioTypeChain.setup();

    return setupChain.obtainAudioType(context);
  }

  private static int calculateScreenBrightnessPercentage(int screenBrightness) {
    return (int) Math.floor(PERCENT_NORMALIZER * screenBrightness / SCREEN_BRIGHTNESS_MAX);
  }
}
