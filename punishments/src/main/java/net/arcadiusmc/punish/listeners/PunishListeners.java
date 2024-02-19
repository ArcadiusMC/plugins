package net.arcadiusmc.punish.listeners;

import static net.arcadiusmc.events.Events.register;

import net.arcadiusmc.punish.PunishPlugin;

public final class PunishListeners {
  private PunishListeners() {}

  public static void registerAll(PunishPlugin plugin) {
    register(new ChannelMessageListener(plugin));
    register(new ChatListener(plugin));
    register(new EavesDropperListener());
    register(new JailListener(plugin));
    register(new JoinListener());
    register(new LoginListener());
    register(new ServerLoadListener(plugin));
    register(new VeinListener(plugin));
  }
}
