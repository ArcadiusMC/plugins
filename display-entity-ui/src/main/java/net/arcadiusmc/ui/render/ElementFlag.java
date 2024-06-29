package net.arcadiusmc.ui.render;

public enum ElementFlag {
  ;

  final int mask;

  ElementFlag() {
    this.mask = 1 << ordinal();
  }
}
