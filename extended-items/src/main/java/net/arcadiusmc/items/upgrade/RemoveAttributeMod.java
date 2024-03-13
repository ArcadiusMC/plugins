package net.arcadiusmc.items.upgrade;

import com.google.common.collect.Multimap;
import java.util.Map.Entry;
import java.util.Objects;
import net.arcadiusmc.items.ExtendedItem;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public record RemoveAttributeMod(String name) implements ItemUpgrade {

  static void remove(String name, ItemMeta meta) {
    Multimap<Attribute, AttributeModifier> modifiers = meta.getAttributeModifiers();

    if (modifiers == null || modifiers.isEmpty()) {
      return;
    }

    for (Entry<Attribute, AttributeModifier> entry : modifiers.entries()) {
      if (!Objects.equals(name, entry.getValue().getName())) {
        continue;
      }

      meta.removeAttributeModifier(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public void apply(ExtendedItem item, ItemMeta meta, ItemStack stack) {
    remove(name, meta);
  }
}
