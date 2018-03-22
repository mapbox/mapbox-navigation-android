package com.mapbox.services.android.navigation.ui.v5.voice.polly;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.services.android.navigation.ui.v5.voice.InstructionListener;
import com.mapbox.services.android.navigation.ui.v5.voice.InstructionPlayer;
import com.mapbox.services.android.navigation.v5.navigation.MapboxSpeech;
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
 * Will retrieve synthesized speech mp3s from Amazon's AWS Polly Service
 * (Requires a valid AWS Cognito Pool ID)
 * </p><p>
 * Will queue each instruction and play them
 * sequentially up until the queue is empty.
 * </p>
 */
public class PollyPlayer implements InstructionPlayer, Callback<ResponseBody> {
  VoiceInstructionLoader voiceInstructionLoader;
  private MediaPlayer pollyMediaPlayer;
  private InstructionListener instructionListener;
  private boolean isMuted;
  private String cacheDirectory;
  Queue<File> instructionQueue;

  /**
   * Construct an instance of {@link PollyPlayer}
   *
   * @param context   to initialize {@link CognitoCachingCredentialsProvider} and {@link AudioManager}
   */
  public PollyPlayer(Context context, Locale locale) {
    this.cacheDirectory = context.getCacheDir().toString();
    instructionQueue = new ConcurrentLinkedQueue();
    voiceInstructionLoader = VoiceInstructionLoader.getInstance();
    voiceInstructionLoader.initialize(
      MapboxSpeech.builder()
        .textType("ssml")
        .language(locale.toString())
        .cache(new Cache(context.getCacheDir(), 10 * 1098 * 1098))
        .accessToken(Mapbox.getAccessToken()));
  }

  /**
   * @param instruction voice instruction to be synthesized and played.
   */
  @Override
  public void play(String instruction) {
    if (!isMuted && !TextUtils.isEmpty(instruction)) {
      getVoiceFile(instruction);
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

  private void getVoiceFile(final String instruction) {
    voiceInstructionLoader.getInstruction(instruction, this);
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
      instructionListener.onError();
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
          instructionListener.onError();
        }
      }
    }).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, responseBody);
  }
}
