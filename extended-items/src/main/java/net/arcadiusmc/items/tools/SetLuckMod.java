package net.arcadiusmc.items.tools;

import net.arcadiusmc.items.ExtendedItem;
import net.arcadiusmc.items.upgrade.UpgradeFunction;
import net.arcadiusmc.text.RomanNumeral;
import net.arcadiusmc.text.TextWriter;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public record SetLuckMod(int level) implements UpgradeFunction {

  @Override
  public void apply(ExtendedItem item, ItemMeta meta, ItemStack stack) {
    PiratesLuck luck = item.getComponent(PiratesLuck.class).orElse(null);

    if (luck == null) {
      return;
    }

    luck.setLevel(level);
  }

  @Override
  public void writePreview(ExtendedItem item, TextWriter writer) {
    writer.line("Pirate's Luck " + RomanNumeral.arabicToRoman(level));
  }
}
