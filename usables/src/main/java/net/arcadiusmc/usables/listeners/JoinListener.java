package net.arcadiusmc.usables.listeners;

import com.google.common.base.Strings;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.usables.Interaction;
import net.arcadiusmc.usables.UsablesPlugin;
import net.arcadiusmc.user.event.UserJoinEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.slf4j.Logger;

class JoinListener implements Listener {

  public static final Logger LOGGER = Loggers.getLogger();

  private final UsablesPlugin plugin;

  public JoinListener(UsablesPlugin plugin) {
    this.plugin = plugin;
  }

  @EventHandler(ignoreCancelled = true)
  public void onUserJoin(UserJoinEvent event) {
    if (!event.isFirstJoin()) {
      return;
    }

    var kitName = plugin.getUsablesConfig().getFirstJoinKit();

    if (Strings.isNullOrEmpty(kitName)) {
      return;
    }

    var kit = plugin.getKits().get(kitName);

    if (kit == null) {
      LOGGER.warn("No kit named '{}' found, cannot give firstJoinKit", kitName);
      return;
    }

    Interaction interaction = kit.createInteraction(event.getPlayer(), true);
    kit.interact(interaction);
  }
}
