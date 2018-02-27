package com.mapbox.services.android.navigation.ui.v5.voice.polly;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.polly.AmazonPollyPresigningClient;
import com.amazonaws.services.polly.model.VoiceId;
import com.mapbox.services.android.navigation.ui.v5.voice.InstructionListener;
import com.mapbox.services.android.navigation.ui.v5.voice.InstructionPlayer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

/**
 * <p>
 * Will retrieve synthesized speech mp3s from Amazon's AWS Polly Service
 * (Requires a valid AWS Cognito Pool ID)
 * </p><p>
 * Will queue each instruction and play them
 * sequentially up until the queue is empty.
 * </p>
 */
public class PollyPlayer implements InstructionPlayer {

  private AmazonPollyPresigningClient pollyClient;
  private MediaPlayer pollyMediaPlayer;
  private List<String> instructionUrls = new ArrayList<>();
  private InstructionListener instructionListener;
  private boolean isMuted;
  private final VoiceId voiceId;

  /**
   * Construct an instance of {@link PollyPlayer}
   *
   * @param context   to initialize {@link CognitoCachingCredentialsProvider} and {@link AudioManager}
   * @param awsPoolId to initialize {@link CognitoCachingCredentialsProvider}
   */
  public PollyPlayer(Context context, String awsPoolId, Locale locale) {
    this.voiceId = getVoiceId(locale);
    initPollyClient(context, awsPoolId);
  }

  /**
   * @param instruction voice instruction to be synthesized and played.
   */
  @Override
  public void play(String instruction) {
    if (!isMuted && !TextUtils.isEmpty(instruction)) {
      executeInstructionTask(instruction);
    }
  }

  @Override
  public boolean isMuted() {
    return isMuted;
  }

  @Override
  public void setMuted(boolean isMuted) {
    this.isMuted = isMuted;
    mutePolly(isMuted);
  }

  @Override
  public void onOffRoute() {
    pauseInstruction();
    clearInstructionUrls();
  }

  @Override
  public void onDestroy() {
    stopPollyMediaPlayerPlaying();
  }

  @Override
  public void addInstructionListener(InstructionListener instructionListener) {
    this.instructionListener = instructionListener;
  }

  private void initPollyClient(Context context, String awsPoolId) {
    CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
      context,
      awsPoolId,
      Regions.US_EAST_1
    );
    pollyClient = new AmazonPollyPresigningClient(credentialsProvider);
  }

  private void mutePolly(boolean isMuted) {
    if (isMuted) {
      stopPollyMediaPlayerPlaying();
      clearInstructionUrls();
    }
  }

  private void stopPollyMediaPlayerPlaying() {
    try {
      if (pollyMediaPlayer != null && pollyMediaPlayer.isPlaying()) {
        pollyMediaPlayer.stop();
        pollyMediaPlayer.release();
        instructionListener.onDone();
      }
    } catch (IllegalStateException exception) {
      Timber.e(exception.getMessage());
    }
  }

  private void executeInstructionTask(String instruction) {
    new InstructionTask(pollyClient, new InstructionTask.TaskListener() {
      @Override
      public void onFinished(String instructionUrl) {
        if (instructionUrls.size() == 0) {
          instructionUrls.add(instructionUrl);
          playInstruction(instructionUrls.get(0));
        } else {
          instructionUrls.add(instructionUrl);
        }
      }

      @Override
      public void onError() {
        if (instructionListener != null) {
          instructionListener.onError();
        }
      }
    }, voiceId).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, instruction);
  }

  private VoiceId getVoiceId(Locale locale) {
    switch (locale.getLanguage()) {
      case "da":
        return VoiceId.Naja;
      case "de":
        return VoiceId.Marlene;
      case "en":
        switch (locale.getCountry()) {
          case "GB":
            return VoiceId.Brian;
          case "AU":
            return VoiceId.Nicole;
          case "IN":
            return VoiceId.Raveena;
          case "CA":
          default:
            return VoiceId.Joanna;
        }
      case "es":
        switch (locale.getCountry()) {
          case "ES":
            return VoiceId.Enrique;
          default:
            return VoiceId.Miguel;
        }
      case "fr":
        return VoiceId.Celine;
      case "it":
        return VoiceId.Giorgio;
      case "nl":
        return VoiceId.Lotte;
      case "pl":
        return VoiceId.Ewa;
      case "pt":
        switch (locale.getCountry()) {
          case "BR":
            return VoiceId.Vitoria;
          default:
            return VoiceId.Ines;
        }
      case "ro":
        return VoiceId.Carmen;
      case "ru":
        return VoiceId.Maxim;
      case "sv":
        return VoiceId.Astrid;
      case "tr":
        return VoiceId.Filiz;
      default:
        return VoiceId.Joanna;
    }
  }

  private void playInstruction(String instruction) {
    if (!TextUtils.isEmpty(instruction)) {
      pollyMediaPlayer = new MediaPlayer();
      setDataSource(instruction);
      pollyMediaPlayer.prepareAsync();
      setListeners();
    }
  }

  private void pauseInstruction() {
    try {
      if (pollyMediaPlayer != null && pollyMediaPlayer.isPlaying()) {
        pollyMediaPlayer.stop();
      }
    } catch (IllegalStateException exception) {
      Timber.e(exception.getMessage());
    }
  }

  private void setDataSource(String instruction) {
    try {
      pollyMediaPlayer.setDataSource(instruction);
    } catch (IOException ioException) {
      Timber.e("Unable to set data source for the media pollyMediaPlayer! %s",
        ioException.getMessage());
    }
  }

  private void setListeners() {
    pollyMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
      @Override
      public void onPrepared(MediaPlayer mp) {
        if (instructionListener != null) {
          instructionListener.onStart();
        }
        mp.start();
      }
    });
    pollyMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
      @Override
      public void onCompletion(MediaPlayer mp) {
        mp.release();
        if (instructionListener != null) {
          instructionListener.onDone();
        }
        onInstructionFinished();
      }
    });
  }

  private void onInstructionFinished() {
    if (instructionUrls.size() > 0) {
      instructionUrls.remove(0);
      if (instructionUrls.size() > 0) {
        playInstruction(instructionUrls.get(0));
      }
    }
  }

  private void clearInstructionUrls() {
    if (instructionUrls.size() > 0) {
      instructionUrls.clear();
    }
  }
}
