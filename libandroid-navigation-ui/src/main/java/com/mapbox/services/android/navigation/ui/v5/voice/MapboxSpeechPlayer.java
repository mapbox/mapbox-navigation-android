package com.mapbox.services.android.navigation.ui.v5.voice;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.mapbox.mapboxsdk.Mapbox;
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
public class MapboxSpeechPlayer implements InstructionPlayer {
  private static final long TEN_MEGABYTE_CACHE_SIZE = 10 * 1098 * 1098;
  private static final String OKHTTP_CACHE = "okhttp_cache";
  private static final String SSML_TEXT_TYPE = "ssml";
  private static final String ERROR_TEXT = "Unable to set data source for the media mediaPlayer! %s";
  private Queue<File> instructionQueue;
  private VoiceInstructionLoader voiceInstructionLoader;
  private MediaPlayer mediaPlayer;
  private InstructionListener instructionListener;
  private boolean isMuted;
  private String cacheDirectory;
  private Cache cache;

  /**
   * Construct an instance of {@link MapboxSpeechPlayer}
   *
   * @param context   to initialize {@link CognitoCachingCredentialsProvider} and {@link AudioManager}
   */
  MapboxSpeechPlayer(Context context, Locale locale) {
    cache = new Cache(new File(context.getCacheDir(), OKHTTP_CACHE), TEN_MEGABYTE_CACHE_SIZE);
    this.cacheDirectory = context.getCacheDir().toString();
    instructionQueue = new ConcurrentLinkedQueue();
    voiceInstructionLoader = VoiceInstructionLoader.builder()
      .language(locale.toString())
      .cache(cache)
      .accessToken(Mapbox.getAccessToken())
      .build();
  }

  void setInstructionListener(InstructionListener instructionListener) {
    this.instructionListener = instructionListener;
  }

  /**
   * Plays the specified text instruction using MapboxSpeech API, defaulting to SSML input type
   *
   * @param instruction voice instruction to be synthesized and played
   */
  @Override
  public void play(String instruction) {
    play(instruction, SSML_TEXT_TYPE);
  }

  /**
   * Plays the specified text instruction using MapboxSpeech API
   *
   * @param instruction voice instruction to be synthesized and played
   * @param textType either "ssml" or "text"
   */
  private void play(String instruction, String textType) {
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
    deleteCache();
  }

  private void deleteCache() {
    try {
      cache.delete();
    } catch (IOException exception) {
      Timber.e(exception.getMessage());
    }
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
        if (instructionListener != null) {
          instructionListener.onDone();
        }
      }
    } catch (IllegalStateException exception) {
      Timber.e(exception.getMessage());
    }
  }

  private void getVoiceFile(final String instruction, String textType) {
    voiceInstructionLoader.getInstruction(instruction, textType, new Callback<ResponseBody>() {
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
    });
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
      Timber.e(ERROR_TEXT, ioException.getMessage());
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
        onInstructionFinishedPlaying();
      }
    });
  }

  private void onInstructionFinishedPlaying() {
    instructionQueue.poll().delete(); // delete the file for the instruction that just finished

    if (!instructionQueue.isEmpty()) {
      playInstruction(instructionQueue.peek().getPath());
    }
  }

  private void clearInstructionUrls() {
    while (!instructionQueue.isEmpty()) {
      instructionQueue.remove().delete();
    }
  }

  private void executeInstructionTask(ResponseBody responseBody) {
    new InstructionDownloadTask(cacheDirectory, new InstructionDownloadTask.TaskListener() {
      @Override
      public void onFinishedDownloading(File instructionFile) {
        if (instructionQueue.isEmpty()) {
          playInstruction(instructionFile.getPath());
        }

        instructionQueue.add(instructionFile);
      }

      @Override
      public void onErrorDownloading() {
        if (instructionListener != null) {
          instructionListener.onError(true);
        }
      }
    }).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, responseBody);
  }
}
