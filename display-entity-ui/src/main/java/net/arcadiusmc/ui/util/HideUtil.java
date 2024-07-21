package net.arcadiusmc.ui.util;

import net.arcadiusmc.utils.PluginUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class HideUtil {
  private HideUtil() {}

  public static void hide(Entity entity) {
    Plugin plugin = PluginUtil.getPlugin();

    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
      onlinePlayer.hideEntity(plugin, entity);
    }
  }

  public static void unhide(Entity entity) {
    Plugin plugin = PluginUtil.getPlugin();

    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
      onlinePlayer.showEntity(plugin, entity);
    }
  }
}
