package net.arcadiusmc.challenges.listeners;

import net.arcadiusmc.challenges.ChallengesPlugin;
import net.arcadiusmc.events.Events;

public final class ChallengeListeners {
  private ChallengeListeners() {}

  public static void registerAll(ChallengesPlugin plugin) {
    var manager = plugin.getChallenges();

    Events.register(new DayChangeListener(manager));
    Events.register(new SellShopListener(manager));
    Events.register(new ServerLoadListener(plugin));
  }
}
