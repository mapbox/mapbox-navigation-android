package com.mapbox.services.android.navigation.v5.osrm;

/**
 * A function to change the raw instruction string before tokens are replaced.
 * Useful to inject custom markup for tokens.
 */

public interface TokenizedInstructionHook {
  String change(String instruction);
}
