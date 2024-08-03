package net.arcadiusmc.items.upgrade;

import net.arcadiusmc.items.ExtendedItem;
import net.arcadiusmc.items.Utils;
import net.arcadiusmc.text.TextWriter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public record AddAttributeMod(
    Attribute attribute,
    String name,
    Operation operation,
    double value
) implements UpgradeFunction {

  @Override
  public void apply(ExtendedItem item, ItemMeta meta, ItemStack stack) {
    RemoveAttributeMod.remove(name, meta);

    AttributeModifier modifier = new AttributeModifier(name, value, operation);
    meta.addAttributeModifier(attribute, modifier);
  }

  @Override
  public void writePreview(ExtendedItem item, TextWriter writer) {
    Component text = Utils.formatAttributeModifier(value, operation, attribute);

    if (text == null) {
      return;
    }

    writer.line(text.color(NamedTextColor.GRAY));
  }

  @Override
  public void writeStatus(ExtendedItem item, TextWriter writer) {
//    Component text = Utils.formatAttributeModifier(value, operation, attribute);
//
//    if (text == null) {
//      return;
//    }
//
//    writer.line(text);
  }

  @Override
  public boolean statusPersists() {
    return false;
  }
}
