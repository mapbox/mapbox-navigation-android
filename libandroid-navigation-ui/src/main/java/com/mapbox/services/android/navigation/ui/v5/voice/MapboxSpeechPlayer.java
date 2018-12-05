package com.mapbox.services.android.navigation.ui.v5.voice;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.text.TextUtils;

import com.mapbox.services.android.navigation.v5.utils.DownloadTask;

import java.io.File;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

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
class MapboxSpeechPlayer implements SpeechPlayer {

  private static final String MAPBOX_INSTRUCTION_CACHE = "mapbox_instruction_cache";
  private static final String ERROR_TEXT = "Unable to set data source for the media mediaPlayer! %s";
  private static final SpeechAnnouncementMap SPEECH_ANNOUNCEMENT_MAP = new SpeechAnnouncementMap();
  private static final String MP3_POSTFIX = "mp3";

  private SpeechAnnouncement announcement;
  private SpeechListener speechListener;
  private MediaPlayer mediaPlayer;
  private Queue<File> instructionQueue;
  private File mapboxCache;
  private boolean isPlaying;
  private boolean isMuted;
  private VoiceInstructionLoader voiceInstructionLoader;

  /**
   * Construct an instance of {@link MapboxSpeechPlayer}
   *
   * @param context                to setup the caches
   * @param voiceInstructionLoader voice instruction loader
   */
  MapboxSpeechPlayer(Context context, @NonNull SpeechListener speechListener,
                     VoiceInstructionLoader voiceInstructionLoader) {
    this.speechListener = speechListener;
    this.voiceInstructionLoader = voiceInstructionLoader;
    setupCaches(context);
    instructionQueue = new ConcurrentLinkedQueue();
  }

  /**
   * Plays the specified text instruction using MapboxSpeech API, defaulting to SSML input type
   *
   * @param announcement with voice instruction to be synthesized and played
   */
  @Override
  public void play(SpeechAnnouncement announcement) {
    boolean isInvalidAnnouncement = announcement == null;
    if (isInvalidAnnouncement) {
      return;
    }
    this.announcement = announcement;
    playAnnouncementTextAndTypeFrom(announcement);
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
    voiceInstructionLoader.flushCache();
  }

  private void setupCaches(Context context) {
    mapboxCache = new File(context.getCacheDir(), MAPBOX_INSTRUCTION_CACHE);
    mapboxCache.mkdir();
  }

  private void playAnnouncementTextAndTypeFrom(SpeechAnnouncement announcement) {
    boolean hasSsmlAnnouncement = announcement.ssmlAnnouncement() != null;
    SpeechAnnouncementUpdate speechAnnouncementUpdate = SPEECH_ANNOUNCEMENT_MAP.get(hasSsmlAnnouncement);
    Pair<String, String> textAndType = speechAnnouncementUpdate.buildTextAndTypeFrom(announcement);
    playAnnouncementText(textAndType.first, textAndType.second);
  }

  private void playAnnouncementText(String instruction, String textType) {
    downloadVoiceFile(instruction, textType);
  }

  private void muteSpeech() {
    if (isMuted) {
      stopMediaPlayerPlaying();
      clearInstructionUrls();
    }
  }

  private void stopMediaPlayerPlaying() {
    if (isPlaying) {
      isPlaying = false;
      mediaPlayer.stop();
      mediaPlayer.release();
      speechListener.onDone();
    }
  }

  private void downloadVoiceFile(final String instruction, String textType) {
    boolean isInvalidInstruction = TextUtils.isEmpty(instruction);
    if (isMuted || isInvalidInstruction) {
      return;
    }

    voiceInstructionLoader.requestInstruction(instruction, textType, new Callback<ResponseBody>() {
      @Override
      public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
        if (response.isSuccessful()) {
          executeInstructionTask(response.body());
        } else {
          try {
            onError(response.errorBody().string());
          } catch (IOException exception) {
            onError(exception.getLocalizedMessage());
          }
        }
      }

      @Override
      public void onFailure(Call<ResponseBody> call, Throwable throwable) {
        onError(throwable.getLocalizedMessage());
      }
    });
  }

  private void onError(String errorText) {
    speechListener.onError(errorText, announcement);
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
    addListeners();
  }

  private void pauseInstruction() {
    if (isPlaying) {
      isPlaying = false;
      mediaPlayer.stop();
    }
  }

  private void setDataSource(String instruction) {
    try {
      mediaPlayer.setDataSource(instruction);
    } catch (IOException ioException) {
      Timber.e(ERROR_TEXT, ioException.getMessage());
    }
  }

  private void addListeners() {
    mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
      @Override
      public void onPrepared(MediaPlayer mp) {
        speechListener.onStart();
        isPlaying = true;
        mp.start();
      }
    });
    mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
      @Override
      public void onCompletion(MediaPlayer mp) {
        mp.release();
        isPlaying = false;
        speechListener.onDone();
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
    new DownloadTask(mapboxCache.getPath(), MP3_POSTFIX, new DownloadTask.DownloadListener() {
      @Override
      public void onFinishedDownloading(@NonNull File instructionFile) {
        playInstructionIfUpNext(instructionFile);
        instructionQueue.add(instructionFile);
      }

      @Override
      public void onErrorDownloading() {
        onError("There was an error downloading the voice files.");
      }
    }).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, responseBody);
  }

  private void playInstructionIfUpNext(File instructionFile) {
    if (instructionQueue.isEmpty()) {
      playInstruction(instructionFile);
    }
  }
}
