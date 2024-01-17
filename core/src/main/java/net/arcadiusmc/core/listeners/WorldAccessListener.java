package net.arcadiusmc.core.listeners;

import net.arcadiusmc.core.CorePlugin;
import net.arcadiusmc.events.WorldAccessTestEvent;
import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class WorldAccessListener implements Listener {

  private final CorePlugin plugin;

  public WorldAccessListener(CorePlugin plugin) {
    this.plugin = plugin;
  }

  @EventHandler(ignoreCancelled = true)
  public void onWorldAccessTest(WorldAccessTestEvent event) {
    String worldName = event.getWorld().getName();
    String[] illegalWorlds = plugin.getFtcConfig().illegalWorlds();

    if (illegalWorlds == null || illegalWorlds.length < 1) {
      return;
    }

    if (!ArrayUtils.contains(illegalWorlds, worldName)) {
      return;
    }

    event.setAccessible(false);
  }
}
