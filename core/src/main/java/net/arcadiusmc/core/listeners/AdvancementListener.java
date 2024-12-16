package net.arcadiusmc.core.listeners;

import java.util.List;
import net.arcadiusmc.command.Commands;
import net.arcadiusmc.core.CorePlugin;
import net.arcadiusmc.core.CustomAdvancementRewards;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.Grenadier;
import org.bukkit.Bukkit;
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

    CustomAdvancementRewards rewards = plugin.getAdvancementRewards();
    List<String> commands = rewards.getCommands(event.getAdvancement());

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
