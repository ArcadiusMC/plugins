package net.arcadiusmc.core.listeners;

import net.arcadiusmc.core.CoreMessages;
import net.arcadiusmc.core.CorePlugin;
import net.arcadiusmc.core.PrefsBook;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.utils.Cooldown;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.title.Title;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

public class DurabilityListener implements Listener {

  static final Sound BREAK_SOUND = Sound.sound(
      org.bukkit.Sound.ENTITY_ITEM_BREAK,
      Sound.Source.MASTER,
      1f, 1f
  );

  private final CorePlugin plugin;

  public DurabilityListener(CorePlugin plugin) {
    this.plugin = plugin;
  }

  @EventHandler(ignoreCancelled = true)
  public void onEntityDamageItem(PlayerItemDamageEvent event) {
    User user = Users.get(event.getPlayer());
    var config = plugin.getCoreConfig();

    if (!user.get(PrefsBook.DURABILITY_WARNINGS) || config.durabilityWarningThreshold() <= 0) {
      return;
    }

    ItemStack item = event.getItem();
    Damageable damageable = (Damageable) item.getItemMeta();

    float damage = damageable.getDamage();
    float maxDurability = item.getType().getMaxDurability();
    float remaining = maxDurability - damage - 1;

    double threshold = config.durabilityWarningThreshold();

    // If durability is above threshold or player is on cooldown, do not show alert
    if (remaining >= (maxDurability * threshold)
        || remaining <= 0
        || Cooldown.containsOrAdd(user, PrefsBook.DURABILITY_WARNINGS.getKey(), 20 * 10)
    ) {
      return;
    }

    user.playSound(BREAK_SOUND);
    user.showTitle(
        Title.title(
            // Title
            CoreMessages.DURABILITY_WARN_TITLE.get()
                .addValue("item", item)
                .addValue("durability", remaining)
                .addValue("maxDurability", maxDurability)
                .create(user),

            // Subtitle
            CoreMessages.DURABILITY_WARN_SUBTITLE.get()
                .addValue("item", item)
                .addValue("durability", remaining)
                .addValue("maxDurability", maxDurability)
                .create(user)
        )
    );
  }
}
