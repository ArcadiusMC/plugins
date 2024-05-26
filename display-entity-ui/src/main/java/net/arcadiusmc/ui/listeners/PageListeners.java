package net.arcadiusmc.ui.listeners;

import lombok.RequiredArgsConstructor;
import net.arcadiusmc.ui.InteractionType;
import net.arcadiusmc.ui.PageInteraction;
import net.arcadiusmc.ui.UiPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@RequiredArgsConstructor
public class PageListeners implements Listener {

  final UiPlugin plugin;

  @EventHandler(ignoreCancelled = true)
  public void onPlayerQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    plugin.getSessions().closeSession(player.getUniqueId());
  }

  @EventHandler(ignoreCancelled = false)
  public void onPlayerInteract(PlayerInteractEvent event) {
    Player player = event.getPlayer();
    InteractionType type;

    Action action = event.getAction();

    if (action.isLeftClick()) {
      type = player.isSneaking() ? InteractionType.SHIFT_LEFT : InteractionType.LEFT;
    } else if (action.isRightClick()) {
      type = player.isSneaking() ? InteractionType.SHIFT_RIGHT : InteractionType.RIGHT;
    } else {
      return;
    }

    plugin.getSessions()
        .getSession(player.getUniqueId())
        .ifPresent(session -> {
          if (session.getTarget() == null) {
            return;
          }

          if (event instanceof Cancellable cancellable) {
            cancellable.setCancelled(true);
          }

          PageInteraction interaction = new PageInteraction(
              session.getTargetPos(),
              session.getScreenPos(),
              type
          );

          session.getTarget().onInteract(interaction);
        });
  }
}
