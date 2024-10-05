package net.arcadiusmc.dungeons.gen;

import net.arcadiusmc.dungeons.Doorway;
import net.arcadiusmc.dungeons.PieceType;

public record StepResult(byte code, Doorway entrance, PieceType type) {

  /**
   * Result code indicating that the result is a success
   */
  public static final byte SUCCESS = 0;

  /**
   * Result code indicating the generation step encountered a fatal failure
   */
  public static final byte FAILED = 1;

  /**
   * Generation step reached or exceeded the maximum dungeon depth
   */
  public static final byte MAX_DEPTH = 2;

  /**
   * Section reached or exceeded the maximum section depth, aka the maximum depth of a specific kind
   * of section, for example: a hallway depth limit
   */
  public static final byte MAX_SECTION_DEPTH = 3;

  public static StepResult failure(byte errorCode) {
    return new StepResult(errorCode, null, null);
  }

  public static StepResult success(Doorway entrance, PieceType type) {
    return new StepResult(SUCCESS, entrance, type);
  }

  public static String codeToString(byte code) {
    return switch (code) {
      case FAILED -> "ERR_FAILED";
      case MAX_DEPTH -> "ERR_MAX_DEPTH";
      case MAX_SECTION_DEPTH -> "ERR_MAX_SECTION_DEPTH";
      case SUCCESS -> "SUCCESS";
      default -> "UNKNOWN(" + code + ")";
    };
  }
}
