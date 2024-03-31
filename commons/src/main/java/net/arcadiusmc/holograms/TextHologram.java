package net.arcadiusmc.holograms;

import org.jetbrains.annotations.Nullable;

public interface TextHologram extends HolographicDisplay {

  void setText(String message);

  @Nullable String getText();
}
