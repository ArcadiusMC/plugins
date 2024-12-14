package net.arcadiusmc.utils;

import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Advancement-related utilities
 */
public final class Advancements {
  private Advancements() {}

  /**
   * Grand a player an advancement
   *
   * @param player Player to grant advancement
   * @param key Resource ID string
   *
   * @return {@code true}, if the {@code key} was a valid resource ID, and was the name of
   *         a registered advancement, {@code false}, otherwise.
   */
  public static boolean grant(@NotNull Player player, @NotNull String key) {
    Objects.requireNonNull(player, "Null player");
    Objects.requireNonNull(key, "Null key");

    NamespacedKey namespacedKey = NamespacedKey.fromString(key);
    if (namespacedKey == null) {
      return false;
    }

    return grant(player, namespacedKey);
  }

  /**
   * Grant a player an advancement
   *
   * @param player Player to grant advancement
   * @param key Advancement resource ID
   *
   * @return {@code true}, if the advancement with the ID was found,
   *         {@code false} if it wasn't.
   */
  public static boolean grant(@NotNull Player player, @NotNull NamespacedKey key) {
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
  public static void grant(@NotNull Player player, @NotNull Advancement advancement) {
    Objects.requireNonNull(player, "Null player");

    AdvancementProgress progress = player.getAdvancementProgress(advancement);
    if (progress.isDone()) {
      return;
    }

    for (String criterion : advancement.getCriteria()) {
      progress.awardCriteria(criterion);
    }
  }
}
