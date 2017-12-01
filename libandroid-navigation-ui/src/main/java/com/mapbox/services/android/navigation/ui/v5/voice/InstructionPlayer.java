package com.mapbox.services.android.navigation.ui.v5.voice;

/**
 * Defines a contract for instruction players
 * used in {@link com.mapbox.services.android.navigation.ui.v5.NavigationView}.
 *
 * @since 0.6.0
 */
public interface InstructionPlayer {

  /**
   * Will play the given string instruction.  If a voice instruction is already playing or
   * other instructions are already queued, the given instruction will be queued to play after.
   *
   * @param instruction voice instruction to be synthesized and played.
   * @since 0.6.0
   */
  void play(String instruction);

  /**
   * @return true if currently muted, false if not
   * @since 0.6.0
   */
  boolean isMuted();

  /**
   * Will determine if voice instructions will be played or not.
   * <p>
   * If called while an instruction is currently playing, the instruction should end immediately and any
   * instructions queued should be cleared.
   *
   * @param isMuted true if should be muted, false if should not
   * @since 0.6.0
   */
  void setMuted(boolean isMuted);

  /**
   * Used in off-route scenarios to stop current
   * instruction (if playing) and voice a rerouting cue.
   *
   * @since 0.6.0
   */
  void onOffRoute();

  /**
   * Used to stop and release the media (if needed).
   *
   * @since 0.6.0
   */
  void onDestroy();

  /**
   * Used to add listener for when instructions begin / end.
   *
   * @since 0.8.0
   */
  void addInstructionListener(InstructionListener instructionListener);
}
