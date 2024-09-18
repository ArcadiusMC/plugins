package net.arcadiusmc.cosmetics;

import net.arcadiusmc.cosmetics.TypeMenuCallback.Noop;

public interface MenuCallbacks {

  @SuppressWarnings("rawtypes")
  TypeMenuCallback DEFAULT = new TypeMenuCallback<>();

  @SuppressWarnings("rawtypes")
  TypeMenuCallback NOP = new Noop<>();

  @SuppressWarnings("unchecked")
  static <T> TypeMenuCallback<T> defaultCallbacks() {
    return DEFAULT;
  }

  @SuppressWarnings("unchecked")
  static <T> TypeMenuCallback<T> nop() {
    return NOP;
  }
}
