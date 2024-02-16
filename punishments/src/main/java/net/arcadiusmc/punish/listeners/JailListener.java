package net.arcadiusmc.punish.listeners;

import com.google.common.base.Strings;
import net.arcadiusmc.punish.JailCell;
import net.arcadiusmc.punish.PunishPlugin;
import net.arcadiusmc.punish.PunishType;
import net.arcadiusmc.text.Messages;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;

class JailListener implements Listener {

  private final PunishPlugin plugin;

  public JailListener(PunishPlugin plugin) {
    this.plugin = plugin;
  }

  JailCell getCell(Player player) {
    return plugin.getPunishManager().getOptionalEntry(player.getUniqueId())
        .map(entry -> entry.getCurrent(PunishType.JAIL))
        .filter(punishment -> !Strings.isNullOrEmpty(punishment.getReason()))
        .flatMap(punishment -> plugin.getJails().getCells().get(punishment.getExtra()))
        .orElse(null);
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerMove(PlayerMoveEvent event) {
    JailCell cell = getCell(event.getPlayer());

    if (cell == null) {
      return;
    }

    Player player = event.getPlayer();

    if (!cell.cellArea().contains(player)) {
      var pos = cell.center();
      player.teleport(new Location(cell.world(), pos.x(), pos.y(), pos.z()));
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
    JailCell cell = getCell(event.getPlayer());

    if (cell == null) {
      return;
    }

    event.setCancelled(true);

    event.getPlayer().sendMessage(
        Messages.renderText("jails.commands", event.getPlayer())
    );
  }
}