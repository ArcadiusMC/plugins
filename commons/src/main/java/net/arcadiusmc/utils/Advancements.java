package net.arcadiusmc.utils;

import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;

/**
 * Advancement-related utilities
 */
public final class Advancements {
  private Advancements() {}

  /**
   * Grant a player an advancement
   *
   * @param player Player to grant advancement
   * @param key Advancement resource ID
   *
   * @return {@code true}, if the advancement with the ID was found,
   *         {@code false} if it wasn't.
   */
  public static boolean grant(Player player, NamespacedKey key) {
    Objects.requireNonNull(key, "Null key");
    Advancement advancement = Bukkit.getAdvancement(key);

    if (advancement == null) {
      return false;
    }

    grant(player, advancement);
    return true;
  }

  /**
   * Grant a player an advancement.
   * @param player Player to give the advancement
   * @param advancement Advancement to grant
   */
  public static void grant(Player player, Advancement advancement) {
    AdvancementProgress progress = player.getAdvancementProgress(advancement);

    if (progress.isDone()) {
      return;
    }

    for (String criterion : advancement.getCriteria()) {
      progress.awardCriteria(criterion);
    }
  }
}
