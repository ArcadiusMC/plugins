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
  public static boolean ENABLED;

  public static Enchantment SOULBOUND;
  public static Enchantment PIRATES_LUCK;
  public static Enchantment IMPERIAL_DUPING;
  public static Enchantment SLICE;
  public static Enchantment CUTTING_MASTERY;
  public static Enchantment BATTLE_CALL;
  public static Enchantment STRONG_AIM;


  static void findEnchantments() {
    SOULBOUND = alertIfNull(EnchantKeys.KEY_SOULBOUND);
    PIRATES_LUCK = alertIfNull(EnchantKeys.KEY_PIRATES_LUCK);
    IMPERIAL_DUPING = alertIfNull(EnchantKeys.KEY_DUPING);
    SLICE = alertIfNull(EnchantKeys.KEY_SLICE);
    CUTTING_MASTERY = alertIfNull(EnchantKeys.KEY_CUTTING_MASTERY);
    BATTLE_CALL = alertIfNull(EnchantKeys.KEY_BATTLE_CALL);
    STRONG_AIM = alertIfNull(EnchantKeys.KEY_STRONG_AIM);

    ENABLED = SOULBOUND != null
        && PIRATES_LUCK != null
        && IMPERIAL_DUPING != null
        && SLICE != null
        && CUTTING_MASTERY != null
        && BATTLE_CALL != null
        && STRONG_AIM != null;
  }

  private static Enchantment alertIfNull(NamespacedKey key) {
    Enchantment ench = Registry.ENCHANTMENT.get(key);

    if (ench == null) {
      LOGGER.error("Couldn't find enchantment '{}'", key);
    }

    return ench;
  }
}
