package net.arcadiusmc.items;

import org.bukkit.NamespacedKey;

public interface EnchantKeys {

  NamespacedKey KEY_SOULBOUND = createKey("soulbound");
  NamespacedKey KEY_PIRATES_LUCK = createKey("pirates_luck");
  NamespacedKey KEY_DUPING = createKey("imperial_duping");
  NamespacedKey KEY_SLICE = createKey("slice");
  NamespacedKey KEY_CUTTING_MASTERY = createKey("cutting_mastery");
  NamespacedKey KEY_BATTLE_CALL = createKey("battle_call");


  private static NamespacedKey createKey(String value) {
    return new NamespacedKey("arcadiusmc", value);
  }
}
