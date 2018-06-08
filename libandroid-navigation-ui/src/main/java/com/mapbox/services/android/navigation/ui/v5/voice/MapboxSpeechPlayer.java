package com.mapbox.services.android.navigation.ui.v5.voice;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.mapbox.services.android.navigation.v5.navigation.VoiceInstructionLoader;

import java.io.File;
import java.io.IOException;
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
  private static final String OKHTTP_INSTRUCTION_CACHE = "okhttp_instruction_cache";
  private static final String MAPBOX_INSTRUCTION_CACHE = "mapbox_instruction_cache";
  private static final String SSML_TEXT_TYPE = "ssml";
  private static final String ERROR_TEXT = "Unable to set data source for the media mediaPlayer! %s";
  private Queue<File> instructionQueue;
  private VoiceInstructionLoader voiceInstructionLoader;
  private MediaPlayer mediaPlayer;
  private InstructionListener instructionListener;
  private boolean isMuted;
  private File mapboxCache;
  private Cache okhttpCache;

  /**
   * Construct an instance of {@link MapboxSpeechPlayer}
   *
   * @param context     to setup the caches
   * @param language    for which language
   * @param accessToken a valid Mapbox access token
   */
  MapboxSpeechPlayer(Context context, String language, String accessToken) {
    setupCaches(context);
    instructionQueue = new ConcurrentLinkedQueue();
    voiceInstructionLoader = VoiceInstructionLoader.builder()
      .language(language)
      .cache(okhttpCache)
      .accessToken(accessToken)
      .build();
  }

  private void setupCaches(Context context) {
    File okHttpDirectory = new File(context.getCacheDir(), OKHTTP_INSTRUCTION_CACHE);
    okHttpDirectory.mkdir();
    okhttpCache = new Cache(okHttpDirectory, TEN_MEGABYTE_CACHE_SIZE);
    mapboxCache = new File(context.getCacheDir(), MAPBOX_INSTRUCTION_CACHE);
    mapboxCache.mkdir();
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
   * @param textType    either "ssml" or "text"
   */
  private void play(String instruction, String textType) {
    downloadVoiceFile(instruction, textType);
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
    flushCache();
  }

  private void flushCache() {
    try {
      okhttpCache.flush();
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

  private void downloadVoiceFile(final String instruction, String textType) {
    boolean isInvalidInstruction = TextUtils.isEmpty(instruction);
    if (isMuted || isInvalidInstruction) {
      return;
    }

    voiceInstructionLoader.getInstruction(instruction, textType, new Callback<ResponseBody>() {
      @Override
      public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
        if (response.isSuccessful()) {
          executeInstructionTask(response.body());
        } else {
          onError();
        }
      }

      @Override
      public void onFailure(Call<ResponseBody> call, Throwable throwable) {
        onError();
      }
    });
  }

  private void onError() {
    if (instructionListener != null) {
      instructionListener.onError(true);
    }
  }

  private void playInstruction(@NonNull File instruction) {
    setupMediaPlayer(instruction.getPath());
  }

  private void setupMediaPlayer(String instructionPath) {
    if (TextUtils.isEmpty(instructionPath)) {
      return;
    }

    mediaPlayer = new MediaPlayer();
    setDataSource(instructionPath);
    mediaPlayer.prepareAsync();
    setListeners();
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
    deleteLastInstructionPlayed();
    startNextInstruction();
  }

  private void deleteLastInstructionPlayed() {
    if (!instructionQueue.isEmpty()) {
      instructionQueue.poll().delete();
    }
  }

  private void startNextInstruction() {
    if (!instructionQueue.isEmpty()) {
      playInstruction(instructionQueue.peek());
    }
  }

  private void clearInstructionUrls() {
    while (!instructionQueue.isEmpty()) {
      instructionQueue.remove().delete();
    }
  }

  private void executeInstructionTask(ResponseBody responseBody) {
    new InstructionDownloadTask(mapboxCache.getPath(), new InstructionDownloadTask.TaskListener() {
      @Override
      public void onFinishedDownloading(@NonNull File instructionFile) {
        playInstructionIfUpNext(instructionFile);
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

  private void playInstructionIfUpNext(File instructionFile) {
    if (instructionQueue.isEmpty()) {
      playInstruction(instructionFile);
    }
  }
}
