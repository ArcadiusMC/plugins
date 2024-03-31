package net.arcadiusmc.usables.objects;

public interface VanillaCancellable extends UsableObject {

  void setCancelVanilla(VanillaCancelState cancelVanilla);

  VanillaCancelState getCancelVanilla();
}
