package net.arcadiusmc.items.lore;

import java.util.Map.Entry;
import net.arcadiusmc.items.ExtendedItem;
import net.arcadiusmc.text.TextWriter;
import org.bukkit.enchantments.Enchantment;

public enum EnchantsLoreElement implements LoreElement {
  ENCHANTS;

  @Override
  public void writeLore(ExtendedItem item, TextWriter writer) {
    for (Entry<Enchantment, Integer> entry : item.getMeta().getEnchants().entrySet()) {
      Enchantment enchantment = entry.getKey();
      int level = entry.getValue();
      writer.line(enchantment.displayName(level));
    }
  }
}
