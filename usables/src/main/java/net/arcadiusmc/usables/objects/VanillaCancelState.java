package net.arcadiusmc.usables.objects;

import net.forthecrown.nbt.BinaryTag;
import net.forthecrown.nbt.BinaryTags;

public enum VanillaCancelState {
  TRUE,
  FALSE,
  IF_TESTS_FAIL,
  IF_TESTS_SUCCEED;

  public static VanillaCancelState load(BinaryTag tag) {
    if (tag == null) {
      return FALSE;
    }

    if (tag.isNumber()) {
      byte num = tag.asNumber().byteValue();
      return switch (num) {
        case 1 -> TRUE;
        case 2 -> IF_TESTS_FAIL;
        case 3 -> IF_TESTS_SUCCEED;
        default -> FALSE;
      };
    }

    if (!tag.isString()) {
      return FALSE;
    }

    String str = tag.asString().value().toLowerCase();

    return switch (str) {
      case "true" -> TRUE;
      case "if_tests_succeed", "on_success" -> IF_TESTS_SUCCEED;
      case "if_tests_fail", "on_fail" -> IF_TESTS_FAIL;
      default -> FALSE;
    };
  }

  public BinaryTag save() {
    return BinaryTags.stringTag(name().toLowerCase());
  }

  public boolean cancelEvent(boolean testsPassed) {
    return switch (this) {
      case TRUE -> true;
      case FALSE -> false;
      case IF_TESTS_FAIL -> !testsPassed;
      case IF_TESTS_SUCCEED -> testsPassed;
    };
  }
}
