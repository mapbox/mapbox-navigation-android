package com.mapbox.services.android.navigation.ui.v5.voice.speech;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.mapbox.api.speech.v1.MapboxSpeech;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.services.android.navigation.ui.v5.voice.InstructionListener;
import com.mapbox.services.android.navigation.ui.v5.voice.InstructionPlayer;
import com.mapbox.services.android.navigation.v5.navigation.VoiceInstructionLoader;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import okhttp3.Cache;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

/**
 * <p>
 * Will retrieve synthesized speech mp3s from Mapbox's API Voice.
 * </p>
 */
public class MapboxSpeechPlayer implements InstructionPlayer, Callback<ResponseBody> {
  private static final long CACHE_SIZE = 10 * 1098 * 1098;
  private static final String SSML_TEXT_TYPE = "ssml";
  VoiceInstructionLoader voiceInstructionLoader;
  private MediaPlayer mediaPlayer;
  private InstructionListener instructionListener;
  private boolean isMuted;
  private String cacheDirectory;
  Queue<File> instructionQueue;

  /**
   * Construct an instance of {@link MapboxSpeechPlayer}
   *
   * @param context   to initialize {@link CognitoCachingCredentialsProvider} and {@link AudioManager}
   */
  public MapboxSpeechPlayer(Context context, Locale locale, InstructionListener instructionListener) {
    this.instructionListener = instructionListener;
    this.cacheDirectory = context.getCacheDir().toString();
    instructionQueue = new ConcurrentLinkedQueue();
    voiceInstructionLoader = VoiceInstructionLoader.getInstance();
    voiceInstructionLoader.initialize(
      MapboxSpeech.builder()
        .language(locale.toString())
        .cache(new Cache(context.getCacheDir(), CACHE_SIZE))
        .accessToken(Mapbox.getAccessToken()));
  }

  /**
   * @param instruction voice instruction to be synthesized and played.
   */
  public void play(String instruction) {
    play(instruction, SSML_TEXT_TYPE);
  }

  public void play(String instruction, String textType) {
    if (!isMuted && !TextUtils.isEmpty(instruction)) {
      getVoiceFile(instruction, textType);
    }
  }

  @Override
  public boolean isMuted() {
    return isMuted;
  }

  @Override
  public void setMuted(boolean isMuted) {
    this.isMuted = isMuted;
    muteSpeech();
  }

  @Override
  public void onOffRoute() {
    pauseInstruction();
    clearInstructionUrls();
  }

  @Override
  public void onDestroy() {
    stopMediaPlayerPlaying();
  }

  private void muteSpeech() {
    if (isMuted) {
      stopMediaPlayerPlaying();
      clearInstructionUrls();
    }
  }

  private void stopMediaPlayerPlaying() {
    try {
      if (mediaPlayer != null && mediaPlayer.isPlaying()) {
        mediaPlayer.stop();
        mediaPlayer.release();
        instructionListener.onDone();
      }
    } catch (IllegalStateException exception) {
      Timber.e(exception.getMessage());
    }
  }

  private void getVoiceFile(final String instruction, String textType) {
    voiceInstructionLoader.getInstruction(instruction, textType, this);
  }

  private void playInstruction(String instruction) {
    if (!TextUtils.isEmpty(instruction)) {
      mediaPlayer = new MediaPlayer();
      setDataSource(instruction);
      mediaPlayer.prepareAsync();
      setListeners();
    }
  }

  private void pauseInstruction() {
    try {
      if (mediaPlayer != null && mediaPlayer.isPlaying()) {
        mediaPlayer.stop();
      }
    } catch (IllegalStateException exception) {
      Timber.e(exception.getMessage());
    }
  }

  private void setDataSource(String instruction) {
    try {
      mediaPlayer.setDataSource(instruction);
    } catch (IOException ioException) {
      Timber.e("Unable to set data source for the media mediaPlayer! %s",
        ioException.getMessage());
    }
  }

  private void setListeners() {
    mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
      @Override
      public void onPrepared(MediaPlayer mp) {
        if (instructionListener != null) {
          instructionListener.onStart();
        }
        mp.start();
      }
    });
    mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
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
    instructionQueue.poll().delete(); // delete the file for the instruction that just finished
    File nextInstruction = instructionQueue.peek();
    if (nextInstruction != null) {
      playInstruction(nextInstruction.getPath());
    }
  }

  private void clearInstructionUrls() {
    while (!instructionQueue.isEmpty()) {
      instructionQueue.remove().delete();
    }
  }

  @Override
  public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
    if (response.isSuccessful()) {
      executeInstructionTask(response.body());
    }
  }

  @Override
  public void onFailure(Call<ResponseBody> call, Throwable throwable) {
    if (instructionListener != null) {
      instructionListener.onError(true);
    }
  }

  private void executeInstructionTask(ResponseBody responseBody) {
    new InstructionTask(cacheDirectory, new InstructionTask.TaskListener() {
      @Override
      public void onFinished(File instructionFile) {
        instructionQueue.add(instructionFile);

        if (instructionQueue.size() == 1) {
          playInstruction(instructionQueue.peek().getPath());
        }
      }

      @Override
      public void onError() {
        if (instructionListener != null) {
          instructionListener.onError(true);
        }
      }
    }).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, responseBody);
  }
}
