package net.arcadiusmc.ui.listeners;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import net.arcadiusmc.ui.InteractionType;
import net.arcadiusmc.ui.PageInteraction;
import net.arcadiusmc.ui.PlayerSession;
import net.arcadiusmc.ui.ScrollDirection;
import net.arcadiusmc.ui.UiPlugin;
import net.arcadiusmc.ui.struct.Document;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@RequiredArgsConstructor
public class PageListeners implements Listener {

  final UiPlugin plugin;

  @EventHandler(ignoreCancelled = true)
  public void onPlayerQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    plugin.getSessions().closeSession(player.getUniqueId());
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerItemHeld(PlayerItemHeldEvent event) {
    Player player = event.getPlayer();
    Optional<PlayerSession> opt = plugin.getSessions().getSession(player.getUniqueId());

    if (opt.isEmpty()) {
      return;
    }

    PlayerSession session = opt.get();
    Document selected = session.getSelected();

    if (selected == null) {
      return;
    }

    ScrollDirection direction;

    int newSlot = event.getNewSlot();
    int prevSlot = event.getPreviousSlot();

    boolean down = newSlot > prevSlot;

    if (Math.abs(newSlot - prevSlot) >= 5) {
      down = !down;
    }

    if (down) {
      direction = ScrollDirection.DOWN;
    } else {
      direction = ScrollDirection.UP;
    }

    if (selected.onScroll(direction)) {
      event.setCancelled(true);
    }
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

          event.setCancelled(true);

          PageInteraction interaction = new PageInteraction(
              session.getTargetPos(),
              session.getScreenPos(),
              type
          );

          session.getTarget().onInteract(interaction);
        });
  }
}
