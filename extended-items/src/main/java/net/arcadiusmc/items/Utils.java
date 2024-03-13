package net.arcadiusmc.items;

import com.google.common.collect.Multimap;
import java.util.Map.Entry;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.TextWriter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

public class Utils {

  static final double KB_RESIST_MULTIPLIER = 10.0d;
  static final double SCALAR_MULTIPLIER = 100.0d;

  public static @Nullable Component formatAttributeModifier(
      double value,
      Operation operation,
      Attribute attribute
  ) {
    if (value == 0) {
      return null;
    }

    double filteredValue;

    if (operation == Operation.ADD_NUMBER) {
      if (attribute == Attribute.GENERIC_KNOCKBACK_RESISTANCE) {
        filteredValue = value * KB_RESIST_MULTIPLIER;
      } else {
        filteredValue = value;
      }
    } else {
      filteredValue = value * SCALAR_MULTIPLIER;
    }

    int translationId = switch (operation) {
      case ADD_NUMBER -> 0;
      case ADD_SCALAR -> 1;
      case MULTIPLY_SCALAR_1 -> 2;
    };

    String translationKey = filteredValue > 0
        ? "attribute.modifier.plus." + translationId
        : "attribute.modifier.take." + translationId;

    TextColor color = filteredValue > 0
        ? NamedTextColor.BLUE
        : NamedTextColor.RED;

    return Component.translatable(
        translationKey,
        color,
        Text.formatNumber(Math.abs(filteredValue)),
        Component.translatable(attribute)
    );
  }

  public static void writeAttributeModifiers(TextWriter writer, ItemMeta meta) {
    Multimap<Attribute, AttributeModifier> modifers
        = meta.getAttributeModifiers(EquipmentSlot.HEAD);

    if (modifers.isEmpty()) {
      return;
    }

    for (Entry<Attribute, AttributeModifier> entry : modifers.entries()) {
      AttributeModifier m = entry.getValue();
      Attribute attribute = entry.getKey();

      Component text = formatAttributeModifier(m.getAmount(), m.getOperation(), attribute);
      writer.line(text);
    }
  }
}
