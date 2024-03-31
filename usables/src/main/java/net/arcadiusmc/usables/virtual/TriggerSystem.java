package net.arcadiusmc.usables.virtual;

public interface TriggerSystem<T extends Trigger> {

  default void initializeSystem(VirtualUsableManager manager) {

  }

  void onTriggerLoaded(VirtualUsable usable, T trigger);

  void onTriggerAdd(VirtualUsable usable, T trigger);

  void onTriggerRemove(VirtualUsable usable, T trigger);
}
