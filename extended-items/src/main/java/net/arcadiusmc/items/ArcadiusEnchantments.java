package net.arcadiusmc.items;

import net.arcadiusmc.Loggers;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.slf4j.Logger;

public final class ArcadiusEnchantments {
  private ArcadiusEnchantments() {}

  private static final Logger LOGGER = Loggers.getLogger();

  /**
   * This boolean will be true, basically only if it's 1.21 and the custom
   * enchants have been added in data packs.
   */
  public static final boolean ENABLED;

  public static final NamespacedKey KEY_SOULBOUND = createKey("soulbound");
  public static final NamespacedKey KEY_PIRATES_LUCK = createKey("pirates_luck");
  public static final NamespacedKey KEY_DUPING = createKey("imperial_duping");
  public static final NamespacedKey KEY_SLICE = createKey("slice");
  public static final NamespacedKey KEY_CUTTING_MASTERY = createKey("cutting_mastery");
  public static final NamespacedKey KEY_BATTLE_CALL = createKey("battle_call");

  public static final Enchantment SOULBOUND;
  public static final Enchantment PIRATES_LUCK;
  public static final Enchantment IMPERIAL_DUPING;
  public static final Enchantment SLICE;
  public static final Enchantment CUTTING_MASTERY;
  public static final Enchantment BATTLE_CALL;

  static {
    SOULBOUND = alertIfNull(KEY_SOULBOUND);
    PIRATES_LUCK = alertIfNull(KEY_PIRATES_LUCK);
    IMPERIAL_DUPING = alertIfNull(KEY_DUPING);
    SLICE = alertIfNull(KEY_SLICE);
    CUTTING_MASTERY = alertIfNull(KEY_CUTTING_MASTERY);
    BATTLE_CALL = alertIfNull(KEY_BATTLE_CALL);

    ENABLED = SOULBOUND != null
        && PIRATES_LUCK != null
        && IMPERIAL_DUPING != null
        && SLICE != null
        && CUTTING_MASTERY != null
        && BATTLE_CALL != null;
  }

  private static NamespacedKey createKey(String value) {
    return new NamespacedKey("arcadiusmc", value);
  }

  private static Enchantment alertIfNull(NamespacedKey key) {
    Enchantment ench = Registry.ENCHANTMENT.get(key);

    if (ench == null) {
      LOGGER.error("Couldn't find enchantment '{}'", key);
    }

    return ench;
  }
}
