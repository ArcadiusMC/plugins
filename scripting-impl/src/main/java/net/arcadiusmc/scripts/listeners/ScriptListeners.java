package net.arcadiusmc.scripts.listeners;

import static net.arcadiusmc.events.Events.register;

import net.arcadiusmc.scripts.ScriptingPlugin;

public final class ScriptListeners {
  private ScriptListeners() {}

  public static void registerAll(ScriptingPlugin plugin) {
    register(new ServerLoadListener(plugin));
  }
}
