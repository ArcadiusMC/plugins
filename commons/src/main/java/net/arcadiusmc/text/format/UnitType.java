package net.arcadiusmc.text.format;

import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.UnitFormat;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

record UnitType(String unit) implements TextFormatType {

  @Override
  public @NotNull Component resolve(@NotNull Object value, @NotNull String style, Audience viewer) {
    if (!(value instanceof Number number)) {
      return Text.valueOf(value, viewer);
    }

    return UnitFormat.unit(number, unit);
  }
}