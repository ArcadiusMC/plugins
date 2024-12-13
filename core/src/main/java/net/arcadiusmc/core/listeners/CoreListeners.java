package net.arcadiusmc.core.listeners;

import static net.arcadiusmc.events.Events.register;

import net.arcadiusmc.core.CorePlugin;

public final class CoreListeners {
  private CoreListeners() {}

  public static void registerAll(CorePlugin plugin) {
    register(new AdminBroadcastListener());
    register(new AdvancementListener(plugin));
    register(new AltLoginListener(plugin));
    register(new ChatHandleListener());
    register(new CoinpileListener());
    register(new DepositListener(plugin));
    register(new DurabilityListener(plugin));
    register(new GamemodeListener());
    register(new HopperListener(plugin));
    register(new IgnoreListListener());
    register(new MobHealthBar(plugin));
    register(new PlayerLoggingListener(plugin));
    register(new PlayerTeleportListener());
    register(new ProjectileLaunchListener());
    register(new ServerListener());
    register(new ServerPingListener());
    register(new SignOwnershipListener());
    register(new TextDecorationListener());
    register(new TrapDoorListener());
  }
}