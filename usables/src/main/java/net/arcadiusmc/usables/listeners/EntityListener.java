package net.arcadiusmc.usables.listeners;

import net.arcadiusmc.usables.Interaction;
import net.arcadiusmc.usables.Usables;
import net.arcadiusmc.usables.objects.UsableEntity;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class EntityListener implements Listener {

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
    var entity = event.getRightClicked();
    var player = event.getPlayer();

    if (!Usables.isUsable(entity)) {
      return;
    }

    UsableEntity usable = Usables.entity(entity);
    usable.load();

    Interaction interaction = usable.createInteraction(player);
    interaction.getContext().put("hand", event.getHand().name().toLowerCase());

    UsablesListeners.execute(usable, interaction, event);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
    if (!(event.getRightClicked() instanceof ArmorStand)) {
      return;
    }

    // Same logic as this listener, but different handler list, so gotta
    // have this method
    onPlayerInteractEntity(event);
  }

}
