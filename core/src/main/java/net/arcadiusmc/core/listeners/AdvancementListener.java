package net.arcadiusmc.core.listeners;

import java.util.List;
import java.util.Map;
import net.arcadiusmc.command.Commands;
import net.arcadiusmc.core.CorePlugin;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.Grenadier;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

public class AdvancementListener implements Listener {

  private final CorePlugin plugin;

  public AdvancementListener(CorePlugin plugin) {
    this.plugin = plugin;
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerAdvancementDone(PlayerAdvancementDoneEvent event) {
    Player player = event.getPlayer();
    NamespacedKey key = event.getAdvancement().getKey();

    Map<NamespacedKey, List<String>> map = plugin.getAdvancementRewards().getRewardMap();

    if (map == null || map.isEmpty()) {
      return;
    }

    List<String> commands = map.get(key);
    if (commands == null || commands.isEmpty()) {
      return;
    }

    CommandSource source = Grenadier.createSource(Bukkit.getConsoleSender()).silent();

    for (String command : commands) {
      String replaced = Commands.replacePlaceholders(command, player);
      Grenadier.enqueueCommand(source, replaced);
    }
  }

}
