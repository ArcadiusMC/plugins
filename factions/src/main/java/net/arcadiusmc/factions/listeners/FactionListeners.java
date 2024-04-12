package net.arcadiusmc.factions.listeners;

import static net.arcadiusmc.events.Events.register;

import github.scarsz.discordsrv.DiscordSRV;
import net.arcadiusmc.factions.Factions;
import net.arcadiusmc.factions.FactionsPlugin;

public class FactionListeners {

  public static void registerAll(FactionsPlugin plugin) {
    register(new MarketsListener(plugin.getManager()));

    if (Factions.isDiscordEnabled()) {
      DiscordListener listener = new DiscordListener(plugin.getManager());
      DiscordSRV.api.subscribe(listener);
    }
  }
}
