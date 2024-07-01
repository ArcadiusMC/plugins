package net.arcadiusmc.ui.render;

public enum RenderLayer {
  CONTENT,
  BACKGROUND,
  OUTLINE,
  ;

  static final RenderLayer[] LAYERS = values();
  static final int LAYER_COUNT = LAYERS.length;
}
