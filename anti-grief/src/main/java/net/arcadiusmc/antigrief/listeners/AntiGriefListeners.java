package net.arcadiusmc.antigrief.listeners;

import static net.arcadiusmc.events.Events.register;

import github.scarsz.discordsrv.DiscordSRV;
import net.arcadiusmc.antigrief.AntiGriefPlugin;

public final class AntiGriefListeners {
  private AntiGriefListeners() {}

  public static void registerAll(AntiGriefPlugin plugin) {
    register(new ChannelMessageListener());
    register(new ChatListener());
    register(new EavesDropperListener());
    register(new JailListener());
    register(new JoinListener());
    register(new LoginListener());
    register(new ServerLoadListener());
    register(new VeinListener(plugin));

    DiscordSRV.api.subscribe(new StaffChatDiscordListener());
  }
}