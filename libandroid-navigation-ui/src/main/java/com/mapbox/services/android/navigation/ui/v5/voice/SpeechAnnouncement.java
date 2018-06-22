package com.mapbox.services.android.navigation.ui.v5.voice;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.mapbox.services.android.navigation.v5.milestone.VoiceInstructionMilestone;

/**
 * Used with {@link NavigationSpeechPlayer#play(SpeechAnnouncement)}.
 * <p>
 * Given either an <tt>ssmlAnnouncement</tt> or <tt>announcement</tt>, the {@link NavigationSpeechPlayer}
 * will first attempt to speak the SSML if {@link MapboxSpeechPlayer} supports the given language.
 * <p>
 * If no SSML announcement is provided and {@link MapboxSpeechPlayer} supports the given language,
 * the player will read the non-null {@link SpeechAnnouncement#announcement()}.
 *
 * @since 0.16.0
 */
@AutoValue
public abstract class SpeechAnnouncement {

  /**
   * Announcment text containing SSML Markup Language
   *
   * @return announcement containing SSML text
   * @see <a href="https://docs.aws.amazon.com/polly/latest/dg/ssml.html">SSML Markup Language</a>
   * @since 0.16.0
   */
  @Nullable
  public abstract String ssmlAnnouncement();

  /**
   * Announcment text without any type of markup.
   *
   * @return announcement text
   * @since 0.16.0
   */
  public abstract String announcement();

  @Nullable
  abstract VoiceInstructionMilestone voiceInstructionMilestone();

  @AutoValue.Builder
  public abstract static class Builder {

    /**
     * Announcment text containing SSML Markup Language
     *
     * @return this builder for chaining options together
     * @see <a href="https://docs.aws.amazon.com/polly/latest/dg/ssml.html">SSML Markup Language</a>
     * @since 0.16.0
     */
    public abstract Builder ssmlAnnouncement(@Nullable String ssmlAnnouncement);

    /**
     * Announcment text without any type of markup.
     *
     * @return this builder for chaining options together
     * @since 0.16.0
     */
    public abstract Builder announcement(String announcement);

    /**
     * The {@link com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener} can provide
     * voice instructions via {@link VoiceInstructionMilestone}.
     *
     * If you pass the milestone into the builder, {@link SpeechAnnouncement} will extract both the SSML
     * and normal speech announcements.
     *
     * @param milestone optional {@link VoiceInstructionMilestone} with SSML / normal announcements
     * @return this builder for chaining options together
     * @since 0.16.0
     */
    public abstract Builder voiceInstructionMilestone(@Nullable VoiceInstructionMilestone milestone);

    @Nullable
    abstract VoiceInstructionMilestone voiceInstructionMilestone();

    abstract SpeechAnnouncement autoBuild();

    public SpeechAnnouncement build() {
      return buildSpeechAnnouncement();
    }

    private SpeechAnnouncement buildSpeechAnnouncement() {
      VoiceInstructionMilestone milestone = voiceInstructionMilestone();
      if (milestone != null) {
        ssmlAnnouncement(milestone.getSsmlAnnouncement());
        announcement(milestone.getAnnouncement());
        return autoBuild();
      } else {
        return autoBuild();
      }
    }
  }

  public static Builder builder() {
    return new AutoValue_SpeechAnnouncement.Builder();
  }
}
