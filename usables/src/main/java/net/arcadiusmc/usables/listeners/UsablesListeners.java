package net.arcadiusmc.usables.listeners;

import static net.arcadiusmc.events.Events.register;
import static net.arcadiusmc.usables.objects.InWorldUsable.CANCEL_VANILLA;

import java.util.Map;
import java.util.Optional;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.usables.Interaction;
import net.arcadiusmc.usables.UsablesPlugin;
import net.arcadiusmc.usables.objects.InWorldUsable;
import net.arcadiusmc.utils.Cooldown;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event.Result;
import org.bukkit.event.player.PlayerInteractEvent;
import org.slf4j.Logger;

public final class UsablesListeners {
  private UsablesListeners() {}

  private static final Logger LOGGER = Loggers.getLogger();

  public static void registerAll(UsablesPlugin plugin) {
    register(new BlockListener());
    register(new ItemListener());
    register(new EntityListener());
    register(new JoinListener(plugin));
    register(new ServerListener());
  }

  static void execute(InWorldUsable usable, Interaction interaction, Cancellable cancellable) {
    Optional<Player> playerOpt = interaction.getPlayer();

    boolean originalCancelState = cancellable.isCancelled();

    if (usable.isCancelVanilla()) {
      cancellable.setCancelled(true);
    }

    if (playerOpt.isPresent()) {
      Player player = playerOpt.get();

      if (player.getGameMode() == GameMode.SPECTATOR) {
        return;
      }

      if (Cooldown.containsOrAdd(player, 5)) {
        return;
      }
    }

    usable.interact(interaction);
    usable.save();

    if (interaction.getBoolean(CANCEL_VANILLA).orElse(usable.isCancelVanilla())) {
      cancellable.setCancelled(true);
    } else {
      cancellable.setCancelled(originalCancelState);
    }
  }

  static void executeInteract(InWorldUsable usable, Player player, PlayerInteractEvent event) {
    Interaction interaction = usable.createInteraction(player);
    Map<String, Object> ctx = interaction.getContext();

    ctx.put("hand", event.getHand());
    ctx.put("useItem", event.useItemInHand());
    ctx.put("useBlock", event.useInteractedBlock());

    execute(usable, interaction, event);

    if (!event.isCancelled()) {
      interaction.getValue("useItem", Result.class).ifPresent(event::setUseItemInHand);
      interaction.getValue("useBlock", Result.class).ifPresent(event::setUseInteractedBlock);
    }
  }
}
